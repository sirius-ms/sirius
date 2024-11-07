(function () {
    d3.select("body")
        .attr("ondragstart", "return false;")
        .attr("ondrop", "return false;")
        .append("div").attr("id", "container");
    window.addEventListener('contextmenu', event => event.preventDefault());
})();

function hasStructure(peak) {
    return (peak?.peakAnnotation?.substructureScore != null)
}

function hasFormula(peak) {
    return (peak?.peakAnnotation?.molecularFormula != null)
}

function isSelectablePeak(peak) {
    //formula-only peaks not selectable in Epimetheus view
    return ((!self.structureView && hasFormula(peak)) || hasStructure(peak));
}

class Base {
    constructor() {
        this.pan = {mouseupCheck: false, mousemoveCheck: false, tolerance: 10, step: 500}
        this.margin = {top: 20, outerRight: 30, innerRight: 20, bottom: 65, left: 60, diff_vertical: 30}
        this.decimal_place = 4
        this.current = {w: undefined, h: undefined}
        this.w;
        this.h;
        this.svg;
        this.peakArea;
        this.tooltip;
        this.idleTimeout;
        this.brush;
        this.zoomX;
        this.x;
        this.xAxis;
        this.domain_fix = {xMin: null, xMax: null}
        this.domain_tmp = {xMin: null, xMax: null, yMax: null}
        this.selected = {hover: null, leftClick: null}
    }

    resize() {
        this.current = {w: window.innerWidth, h: window.innerHeight};
        this.w = this.current.w - this.margin.left - this.margin.outerRight;
        this.h = this.current.h - this.margin.top - this.margin.bottom;
    }

    build() {
        d3.select("#container").html("");
        this.resize();
        this.svg = d3.select("#container")
            .append('svg')
            .attr("id", "spectrumView")
            .attr("height", this.current.h)
            .attr("width", this.current.w)
            .append("g")
            .attr("id", "content")
            .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");
        // X label
        this.svg.append("text")
            .attr("class", "label spectrum_label")
            .attr("id", "xLabel")
            .attr("x", this.w / 2)
            .attr("y", this.h + this.margin.top + 20)
            .text("m/z");
        // Y label
        this.svg.append("text")
            .attr("class", "label spectrum_label")
            .attr("id", "yLabel")
            .attr("transform", "rotate(-90)")
            .attr("y", -40)
            .attr("x", -this.h / 2)
            .text("Relative Intensity");

        this.svg.selectAll(".label").attr("visibility", "hidden");
        //tooltip
        this.tooltip = d3.select("#container")
            .append("div")
            .attr("id", "tooltip")
            .style("opacity", 0)
        //clipPath & brushing
        this.svg.append("defs").append("svg:clipPath")
            .attr("id", "clip")
            .append("svg:rect")
            .attr("id", "clipArea")
            .attr("width", this.w)
            .attr("height", this.h)
            .attr("x", 0)
            .attr("y", 0);

        this.peakArea = this.svg.append("g")
            .attr("id", "peaks")
            .attr("clip-path", "url(#clip)");
        // shadow peaks for selection and hover (not for mirror plot)
        this.peakArea.selectAll()
            .data(["shadow_select", "shadow_hover"])
            .enter()
            .append("rect")
            .attr("x", 0)
            .attr("y", 0)
            .attr("height", this.h)
            .attr("id", function (d) {return d;})
            .attr("class", "peak_shadow")
            .attr("visibility", "hidden");
        this.peakArea.append("g")
            .attr("id", "brushArea");
    }

    translateHover(mouse_w, mouse_h) {
        // NOTE: These distances might need to be changed, when the font size and content of hover are changed.
        // current hover style: padding = 5px, border-width = 1px
        let tmp_tooltip = document.querySelector("#tooltip");
        let tmp_h = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("height")) + 12;
        let tmp_w = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("width")) + 12;
        const distanceVertical = 30, distanceHorizontal = 30;
        // style: vertical distance between cursor and hover (depending on size of cursor)
        if (mouse_h + tmp_h + distanceVertical > this.current.h) {
            this.tooltip.style("top", (mouse_h - distanceVertical - tmp_h / 2 + "px"));
        } else {
            this.tooltip.style("top", (mouse_h - distanceVertical + "px"));
        }
        // style: horizontal distance between cursor and hover (depending on size of cursor)
        if (mouse_w + tmp_w + distanceHorizontal > this.current.w) {
            this.tooltip.style("left", (mouse_w - distanceHorizontal - tmp_w + "px"));
        } else {
            this.tooltip.style("left", (mouse_w + distanceHorizontal + "px"));
        }
    }

    hideHover() {
        this.tooltip.style("opacity", 0);
        this.tooltip.html("");
    }

    static annotation(self, d) {
        let anno = "";
        if (hasFormula(d)) {
            let sign = (d.peakAnnotation.massDeviationMz > 0) ? "+" : "";
            anno = "Formula: " + d.peakAnnotation.molecularFormula +
                "<br>Intensity: " + d.intensity.toFixed(self.decimal_place) +
                "<br>m/z: " + d.mz.toFixed(self.decimal_place);
            if ("massDeviationMz" in d.peakAnnotation) {
                anno = anno + "<br>Mass deviation: " + sign + (d.peakAnnotation.massDeviationMz * 1000).toFixed(self.decimal_place) + " mDa<br>" +
                    "&nbsp;".repeat(25) + "(" + sign + d.peakAnnotation.massDeviationPpm.toFixed(self.decimal_place) + " ppm)";
            }
            if (hasStructure(d)) {
                anno = anno + "<br>Fragmenter Score: " + d.peakAnnotation.substructureScore.toFixed(2);
            }
        } else {
            anno = "m/z: " + d.mz.toFixed(self.decimal_place) + "<br>Intensity: " + d.intensity.toFixed(self.decimal_place);
        }
        return anno;
    }

    /*
    Kai: takes the spectrum and the x-domain and returns a mouse event listener which calls the given callback with the peak
    index as argument whenever the mouse comes close to a peak
    */
    static mouseMoving(spectrum, xdomain, ydomain, callbackIn, callbackLeave, tolerance = 40) {
        const mzvalues = spectrum.peaks.map(d => d.mz);
        var lastPeakSelected = -1;
        return function () {
            event = d3.event; // Kai: -_- old d3 version
            event.preventDefault();
            const mousePointer = d3.mouse(document.getElementById("peaks"));
            const mzcurrent = xdomain.invert(mousePointer[0] - tolerance);
            let closestPeakIndex = -1;
            let bestDist = Infinity;
            for (let k = Math.max(0, d3.bisect(mzvalues, mzcurrent) - 1); k < mzvalues.length; ++k) {
                const mzp = xdomain(mzvalues[k]);
                if (mzp > mousePointer[0] + tolerance) {
                    break;
                }
                if (mzp >= mousePointer[0] - tolerance) {
                    const intens = ydomain(spectrum.peaks[k].intensity / 2.0);
                    const distance = Math.abs(mzp - mousePointer[0]) + (Math.abs(intens - mousePointer[1])) / 2.0;
                    if (distance < bestDist) {
                        bestDist = distance;
                        closestPeakIndex = k;
                    }
                }
            }
            const selection = closestPeakIndex;
            if (selection === lastPeakSelected && lastPeakSelected === -1) callbackLeave(); // Wei: In mirror plot, somehow it never leaves...So I add this line.
            if (selection != lastPeakSelected) {
                lastPeakSelected = selection;
                if (selection >= 0) {
                    callbackIn(selection);
                } else {
                    callbackLeave();
                }
            }
        };
    }

    static mousemoveGeneral(self, d, i) {
        let event = window.event;
        self.translateHover(event.clientX, event.clientY);
        self.tooltip.html(Base.annotation(self, d));
        self.tooltip.style("opacity", 1);
        const hoveredPeak = d3.select("#peak" + i);
        hoveredPeak.classed("peak_hover", true);
        Base.setXandCenterPeak(self, hoveredPeak);
        if (self.selected.hover !== i) {
            const peak = d3.select("#peak" + self.selected.hover);
            peak.classed("peak_hover", false);
            Base.setXandCenterPeak(self, peak);
            self.selected.hover = i;
        }
    }

    static mouseleaveGeneral(self) {
        if (self.selected.hover !== null) {
            const peak = d3.select("#peak" + self.selected.hover);
            peak.classed("peak_hover", false);
            Base.setXandCenterPeak(self, peak);
            self.selected.hover = null;
            self.hideHover();
        }
    }

    static rightClickOnly() {
        d3.select("#shadow_hover").attr("visibility", "hidden");
        return d3.event.button === 2;
    }

    static brushendX(self, xdomain_fix, duration, ...callbackUpdates) {
        let extent = d3.event.selection;
        if (!extent) {
            if (!self.idleTimeout) return self.idleTimeout = setTimeout(function () {
                self.idleTimeout = null;
            }, 350);
            self.x.domain([xdomain_fix[0], xdomain_fix[1]])
            self.domain_tmp.xMin = xdomain_fix[0];
            self.domain_tmp.xMax = xdomain_fix[1];
        } else {
            self.domain_tmp.xMin = self.x.invert(extent[0]);
            self.domain_tmp.xMax = self.x.invert(extent[1]);
            self.x.domain([self.domain_tmp.xMin, self.domain_tmp.xMax])
            self.peakArea.select("#brushArea").call(self.brush.move, null);
        }
        self.xAxis.transition().duration(duration).call(d3.axisBottom(self.x));
        callbackUpdates.forEach(function (callback) {
            callback(self, duration);
        });
    }

    static zoomedX(self, xdomain_fix, duration, ...callbackUpdates) {
        const scale_tmp_x = d3.event.transform.rescaleX(self.x);
        const newDomain = d3.axisBottom(scale_tmp_x).scale().domain();
        self.domain_tmp.xMin = (newDomain[0] < xdomain_fix[0]) ? xdomain_fix[0] : newDomain[0];
        self.domain_tmp.xMax = (newDomain[1] > xdomain_fix[1]) ? xdomain_fix[1] : newDomain[1];
        self.x.domain([self.domain_tmp.xMin, self.domain_tmp.xMax])
        self.xAxis.transition().duration(duration).call(d3.axisBottom(self.x));
        self.peakArea.select("#brushArea").node().__zoom = d3.zoomIdentity;
        callbackUpdates.forEach(function (callback) {
            callback(self, duration);
        });
    }

    static setXdomain(self, newXmin, newXmax, duration) {
        self.x.domain([newXmin, newXmax])
        self.domain_tmp.xMin = newXmin;
        self.domain_tmp.xMax = newXmax;
        self.xAxis.transition().duration(duration).call(d3.axisBottom(self.x));
    }

    static panX(self, selection, xdomain_fix, duration, mouseUpDuration, mouseUpCallback, ...callbackUpdates) {
        if (d3.event.button === 0) {
            var div = selection;
            var w = d3.select(window)
                .on("mousedown", mousedownPan)
                .on("mousemove", mousemovePan)
                .on("mouseup", mouseupPan);
            d3.event.preventDefault(); // disable text dragging
            var x0, x1, d, newXmin, newXmax;

            function mousedownPan() {
                if (div.node().id === 'brushArea') {
                    self.pan.mouseupCheck = true;
                    x0 = d3.event.clientX;
                }
            };

            function mousemovePan() {
                if (self.pan.mouseupCheck) {
                    x1 = d3.event.clientX;
                    d = x1 - x0;
                    if (Math.abs(d) >= self.pan.tolerance) {
                        self.pan.mousemoveCheck = true;
                        newXmin = self.domain_tmp.xMin - d * (self.domain_tmp.xMax - self.domain_tmp.xMin) / self.pan.step;
                        newXmax = self.domain_tmp.xMax - d * (self.domain_tmp.xMax - self.domain_tmp.xMin) / self.pan.step;
                        if (newXmin >= xdomain_fix[0] && newXmax <= xdomain_fix[1]) {
                            Base.setXdomain(self, newXmin, newXmax, duration);
                            callbackUpdates.forEach(function (callback) {
                                callback(self, duration);
                            });
                        }
                        x0 = x1;
                        d = 0;
                    }
                }
            };

            function mouseupPan() {
                if (self.pan.mouseupCheck && self.pan.mousemoveCheck) {
                    w.on("mousedown", null).on("mousemove", null).on("mouseup", null);
                    mouseUpCallback(self, mouseUpDuration);
                    callbackUpdates.forEach(function (callback) {
                        callback(self, mouseUpDuration);
                    });
                }
            };
        }
    }

    static zoomedY(self, minIntensity, duration, ...callbackUpdates) {
        const scale_tmp_y = d3.event.transform.rescaleY(self.y);
        const newDomain = d3.axisBottom(scale_tmp_y).scale().domain();
        self.domain_tmp.yMax = (newDomain[1] > 1) ? 1 : (newDomain[1] > minIntensity) ? newDomain[1] : minIntensity;
        self.y.domain([0, self.domain_tmp.yMax])
        self.yAxis.transition().duration(duration).call(d3.axisLeft(self.y));
        d3.select("#zoomAreaY").node().__zoom = d3.zoomIdentity;
        callbackUpdates.forEach(function (callback) {
            callback(self, duration);
        });
    }

    static setXandCenterPeak(self, peak) {
        peak.attr("x", function(d) {
            var rectWidth = this.getBoundingClientRect().width;
            return self.x(d.mz) - rectWidth / 2;
        });
    }
}

class SpectrumPlot extends Base {
    constructor(data, svg_str, structureView) {
        super();
        this.build();
        this.data = data;
        this.svg_str = svg_str;
        this.structureView = structureView;
        this.spectrum = data.spectra[0];
        this.peakMatchArr = data.peakMatches?.[0];
        this.peakMatches = new Map(this.spectrum.peaks.map((p, i) => [p, this.peakMatchArr?.[i]]));
        this.mzs = this.spectrum.peaks.map(d => d.mz);
        this.mzsSize = this.mzs.length;
        if (this.spectrum.precursorMz) {
            this.domain_fix.xMin = 0;
            this.domain_fix.xMax = this.spectrum.precursorMz + 5;
        } else {
            this.domain_fix.xMin = d3.min(this.mzs) - 3;
            this.domain_fix.xMax = d3.max(this.mzs) + 3;
        }
        this.y;
        this.yAxis;
        this.zoomY;
        this.zoomAreaY;
        this.basic_structure;
        this.strucArea;
        this.annoArea;
    }

    get leftClickSelected() {
        return this.selected.leftClick;
    }

    static resetColor(self, peakData) {
        if (peakData !== undefined) {
            if (self.spectrum.name.includes("MS1")) {
                return (self.peakMatches.get(peakData) != null) ? "peak_matched peak" : "peak";
            } else {
                if (self.structureView && hasStructure(peakData)) {
                    return "peak_formulaInfo peak_structInfo peak";
                } else {
                    return hasFormula(peakData) ? "peak_formulaInfo peak" : "peak";
                }
            }
        }
    }

    static update_y(self, duration) {
        const maxInt = d3.max(self.spectrum.peaks.filter(d => self.domain_tmp.xMin <= d.mz && d.mz <= self.domain_tmp.xMax), (d) => d.intensity);
        if (maxInt !== undefined && maxInt > 0) {
            self.domain_tmp.yMax = maxInt;
            self.y.domain([0, self.domain_tmp.yMax]);
            self.yAxis.transition().duration(duration).call(d3.axisLeft(self.y));
        }
    }

    static update_peaks(self, duration) {
        self.peakArea.selectAll(".peak").transition().duration(duration)
            .attr("x", function (d) {
                var rectWidth = this.getBoundingClientRect().width;
                return self.x(d.mz) - rectWidth / 2;
            })
            .attr("y", function (d) {
                return (self.y(d.intensity) < 0) ? 0 : self.y(d.intensity);
            })
            .attr("height", function (d) {
                return (self.h - self.y(d.intensity) >= self.h) ? self.h : self.h - self.y(d.intensity);
            });
        if (self.selected.leftClick !== -1 && self.selected.leftClick !== null) {
            SpectrumPlot.update_shadow(self, "#shadow_select", self.selected.leftClick, duration);
        }
    }

    static update_shadow(self, id, i, duration) {
        var peak = self.peakArea.select(id);
        var rectWidth = peak.node().getBoundingClientRect().width;
        peak.transition().duration(duration)
            .attr("x", self.x(self.spectrum.peaks[i].mz) - rectWidth / 2)
            .attr("visibility", "visible");
    }

    static selectNewPeak(self, d, i, newPeak) {
        if (self.selected.leftClick !== -1 && self.selected.leftClick !== null && !newPeak.classed("peak_select")) {
            var deselectedPeak = d3.select("#peak" + self.selected.leftClick);
            deselectedPeak.attr("class", SpectrumPlot.resetColor(self, self.spectrum.peaks[self.selected.leftClick]));
            Base.setXandCenterPeak(self, deselectedPeak);
        }
        try {
            connector.selectionChanged(self.mzs[i]);
        } catch (error) {
            null;
        }
        self.selected.leftClick = i;
        newPeak.classed("peak_select", true);
        Base.setXandCenterPeak(self, newPeak);

        if (self.structureView) {
            self.annoArea.attr("id", "anno_leftClick");
            document.getElementById("anno_leftClick").innerText = Base.annotation(self, d).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
            if (hasFormula(d)) { //showStructure method handles formula-only peaks without structure
                SpectrumPlot.showStructure(self, i);
            } else {
                SpectrumPlot.showStructure(self, -1);
            }
        }
        self.hideHover();
        self.peakArea.select("#shadow_hover").attr("visibility", "hidden");
    }

    static cancelSelection(self, peak) {
        try {
            connector.selectionChanged(-1);
        } catch (error) {
            null;
        }
        self.selected.leftClick = null;
        peak.classed("peak_select", false);
        Base.setXandCenterPeak(self, peak);
        self.peakArea.select("#shadow_select").attr("visibility", "hidden");
        if (self.structureView) {
            document.getElementById("anno_leftClick").innerText = "Left click to choose a green or black peak...";
            self.annoArea.attr("id", "nothing");
            SpectrumPlot.showStructure(self, -1);
        }
    }

    static mouseup(self, d, i) {
        if (!self.pan.mousemoveCheck) {
            if (isSelectablePeak(d)) {
                let tmp = d3.select("#peak" + i);
                if (self.selected.leftClick !== null && tmp.classed("peak_select")) {
                    SpectrumPlot.cancelSelection(self, tmp);
                } else {
                    SpectrumPlot.selectNewPeak(self, d, i, tmp);
                    SpectrumPlot.update_shadow(self, "#shadow_select", i, 0);
                }
            }
        }
        self.pan.mouseupCheck = false;
        self.pan.mousemoveCheck = false;
    }

    static keyDown(self, e) {
        if (self.selected.leftClick !== null) {
            var selectedPeak, new_selected = -1;
            if (e.keyCode === 37 && self.selected.leftClick !== 0) { // left
                new_selected = self.selected.leftClick - 1;
                selectedPeak = self.spectrum.peaks[new_selected];
                while (!isSelectablePeak(selectedPeak)) {
                    if (new_selected === 0) {
                        new_selected = -1;
                        break;
                    }
                    new_selected = new_selected - 1;
                    selectedPeak = self.spectrum.peaks[new_selected];
                }
            } else if (e.keyCode === 39 && self.selected.leftClick !== self.mzsSize - 1) { // right
                new_selected = self.selected.leftClick + 1;
                selectedPeak = self.spectrum.peaks[new_selected];
                while (!isSelectablePeak(selectedPeak)) {
                    if (new_selected === self.mzsSize - 1) {
                        new_selected = -1;
                        break;
                    }
                    new_selected = new_selected + 1;
                    selectedPeak = self.spectrum.peaks[new_selected];
                }
            }
            if (new_selected !== -1) {
                const lastPeak = self.svg.select("#peak" + self.selected.leftClick)
                lastPeak.attr("class", SpectrumPlot.resetColor(self, self.spectrum.peaks[self.selected.leftClick]));
                if (self.selected.leftClick === self.selected.hover) {
                    lastPeak.classed("peak_hover", true);
                }
                try {
                    connector.selectionChanged(self.mzs[new_selected]);
                } catch (error) {
                    null;
                }
                Base.setXandCenterPeak(self, lastPeak);
                self.selected.leftClick = new_selected;
                const newPeak = self.svg.select("#peak" + self.selected.leftClick);
                newPeak.classed("peak_select", true);
                Base.setXandCenterPeak(self, newPeak);

                if (self.selected.leftClick !== -1 && self.selected.leftClick !== null) {
                    SpectrumPlot.update_shadow(self, "#shadow_select", self.selected.leftClick, 0);
                }

                if (selectedPeak.mz <= self.domain_tmp.xMin) {
                    Base.setXdomain(self, selectedPeak.mz - 3, self.domain_tmp.xMax - (self.domain_tmp.xMin - selectedPeak.mz) - 3);
                    SpectrumPlot.update_peaks(self, 50);
                } else if (selectedPeak.mz >= self.domain_tmp.xMax) {
                    Base.setXdomain(self, self.domain_tmp.xMin + (selectedPeak.mz - self.domain_tmp.xMax) + 3, selectedPeak.mz + 3);
                    SpectrumPlot.update_peaks(self, 50);
                }

                if (self.structureView) {
                    document.getElementById("anno_leftClick").innerText = Base.annotation(self, selectedPeak).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
                    if (hasFormula(self.spectrum.peaks[self.selected.leftClick])) { //SpectrumPlot.showStructure method handles formula-only peaks without structure
                        SpectrumPlot.showStructure(self, self.selected.leftClick);
                    } else {
                        SpectrumPlot.showStructure(self, -1);
                    }
                }
            }
        }
    }

    initStructureView() {
        var self = this;
        this.strucArea = d3.select("#container")
            .append('div')
            .attr("id", "structureView")
            .style("height", this.current.h + "px")
            .style("width", this.w / 4 + "px")
            .style("right", this.margin.outerRight + "px")
            .append('div')
            .attr("id", "str_border")
            .style("height", this.w / 4 + "px")
            .style("width", this.w / 4 + "px");
        let anno_top = this.w / 4 + 10; // because padding=5
        this.annoArea = d3.select("#structureView")
            .append('p')
            .attr("class", "anno")
            .attr("id", function () {
                return (self.selected.leftClick !== null) ? "anno_leftClick" : "nothing";
            })
            .style("top", anno_top + "px")
            .text(function () {
                if (self.selected.leftClick !== null) {
                    return Base.annotation(self, self.spectrum.peaks[self.selected.leftClick]).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
                } else {
                    return "Left click to choose a green or black peak...";
                }
            });
        this.current.w = this.current.w / 4 * 3 - 15;
        this.w = this.current.w - this.margin.left - this.margin.innerRight;
        d3.select("#spectrumView").attr("width", this.current.w + "px");
        d3.select("#xLabel").attr("x", this.w / 2);
        this.svg.select("#clipArea").attr("width", this.w);
    }

    static showStructure(self, i) {
        function removeElementStyle(className, styleName) {
            let elements = document.getElementsByClassName(className);
            for (let e of elements) {
                if (e.nodeName === 'g') {
                    let children = e.children;
                    for (let child of children) {
                        child.classList.add(className);
                        if (child.hasAttribute(styleName)) child.removeAttribute(styleName);
                    }
                } else {
                    if (e.hasAttribute(styleName)) e.removeAttribute(styleName);
                }
            }
        };

        function highlighting(self, bonds, cuts, atoms) {
            let totalBond = d3.selectAll(".bond").size();
            let total = new Set(Array.from({length: totalBond}, (v, i) => i));
            let bond_set = new Set(bonds);
            let cut_set = new Set(cuts);
            let rest = new Set([...total].filter(x => !bond_set.has(x)));
            rest = new Set([...rest].filter(x => !cut_set.has(x)));
            for (let i in bonds) {
                self.strucArea.select("#mol1bnd" + (bonds[i] + 1)).attr("class", "bond highlight_bond");
            }
            for (let j in cuts) {
                self.strucArea.select("#mol1bnd" + (cuts[j] + 1)).attr("class", "bond highlight_cut");
            }
            for (let l of rest) {
                self.strucArea.select("#mol1bnd" + (l + 1)).attr("class", "bond rest_bond");
            }
            let highlight_atoms = new Set(atoms);
            let paths = document.getElementsByClassName("atom");
            for (let p of paths) {
                let id = Number(p.id.replace("mol1atm", "")) - 1;
                if (highlight_atoms.has(id)) {
                    p.setAttribute('class', "atom highlight_atom");
                } else {
                    p.setAttribute('class', "atom rest_atom");
                }
            }
        };
        self.strucArea.html("");
        document.getElementById('str_border').innerHTML = self.svg_str;
        self.basic_structure = self.strucArea.select('svg').style('height', '100%').style('width', '100%');
        removeElementStyle("bond", "stroke");
        removeElementStyle("atom", "fill");
        if (i === -1) {
            self.strucArea.selectAll(".bond").classed("default_bond", true);
            self.strucArea.selectAll(".atom").classed("default_atom", true);
            self.basic_structure.style("opacity", 1);
        } else if (i === self.mzsSize - 1) {
            self.strucArea.selectAll(".bond").classed("highlight_bond", true);
            self.strucArea.selectAll(".atom").classed("highlight_atom", true);
            self.basic_structure.style("opacity", 1);
        } else if (hasStructure(self.spectrum.peaks[i])) {
            const peakAnno = self.spectrum.peaks[i].peakAnnotation;
            highlighting(self, peakAnno.substructureBonds, peakAnno.substructureBondsCut, peakAnno.substructureAtoms);
            self.basic_structure.style("opacity", 1);
        } else if (hasFormula(self.spectrum.peaks[i])) {
            //only formula but no structure. Show as 'background'
            self.strucArea.selectAll(".bond").classed("rest_bond", true);
            self.strucArea.selectAll(".atom").classed("rest_atom", true);
            self.basic_structure.style("opacity", 1);
        } else { // Just for error: it can't be other value
            self.basic_structure.style("opacity", 0);
        }
    }

    static setSelection(self, mz) {
        var i;
        for (i in self.mzs) {
            if (Math.abs(self.mzs[i] - mz) < 1e-3) break;
        }
        const d = self.spectrum.peaks[i];
        i = Number(i);
        if (self.selected.leftClick !== i) {
            if (hasFormula(d)) {
                SpectrumPlot.selectNewPeak(self, d, i, d3.select("#peak" + i));
                if (mz <= self.domain_tmp.xMin) {
                    Base.setXdomain(self, mz - 3, self.domain_tmp.xMax - (self.domain_tmp.xMin - mz) - 3);
                    SpectrumPlot.update_peaks(self, 50);
                } else if (mz >= self.domain_tmp.xMax) {
                    Base.setXdomain(self, self.domain_tmp.xMin + (mz - self.domain_tmp.xMax) + 3, mz + 3);
                    SpectrumPlot.update_peaks(self, 50);
                }
            } else if (self.selected.leftClick !== null) SpectrumPlot.cancelSelection(d3.select("#peak" + self.selected.leftClick));
        }
    }

    plot() {
        var self = this;
        if (this.structureView) this.initStructureView();
        // X axis
        if (this.domain_tmp.xMin === null || this.domain_tmp.xMax === null) {
            this.x = d3.scaleLinear().range([0, this.w]).domain([this.domain_fix.xMin, this.domain_fix.xMax]);
            this.domain_tmp.xMin = this.domain_fix.xMin;
            this.domain_tmp.xMax = this.domain_fix.xMax;
        } else {
            this.x = d3.scaleLinear().range([0, this.w]).domain([this.domain_tmp.xMin, this.domain_tmp.xMax]);
        }
        this.xAxis = this.svg.append("g")
            .attr("id", "xAxis")
            .attr("transform", "translate(0," + this.h + ")")
            .call(d3.axisBottom(this.x));
        // Y axis
        const maxIntensity = d3.max(this.spectrum.peaks.map(d => d.intensity)) || 1.0;
        this.y = d3.scaleLinear().domain([0, maxIntensity]).range([this.h, 0]).nice();
        this.domain_tmp.yMax = maxIntensity;
        this.yAxis = this.svg.append("g").attr("id", "yAxis").call(d3.axisLeft(this.y));
        this.svg.selectAll(".label").attr("visibility", "visible");
        // zoom and pan (X-axis)
        this.zoomX = d3.zoom().extent([[0, 0], [this.w, this.h]])
            .on("zoom", function () {
                Base.zoomedX(self, [0, self.domain_fix.xMax], 100, SpectrumPlot.update_y, SpectrumPlot.update_peaks);
            });
        this.peakArea.select("#brushArea").call(this.zoomX)
            .on("dblclick.zoom", null)
            .on("mousedown.zoom", function () {
                var selection = d3.select(this);
                Base.panX(self, selection, [0, self.domain_fix.xMax], 100, 300, SpectrumPlot.update_y, SpectrumPlot.update_peaks);
            });
        // zoom and pan (Y-axis)
        this.zoomAreaY = d3.select("#container")
            .append("div")
            .attr("id", "zoomAreaY")
            .style("position", "absolute")
            .style("left", 0 + "px")
            .style("top", this.margin.top + "px")
            .style("width", this.margin.left + "px")
            .style("height", this.h + "px");
        const minIntensity = d3.min(this.spectrum.peaks.map(d => d.intensity));
        this.zoomY = d3.zoom().on("zoom", function () {
            Base.zoomedY(self, minIntensity, 100, SpectrumPlot.update_peaks);
        });
        this.zoomAreaY.call(this.zoomY).on("dblclick.zoom", null);
        // brush (X-axis)
        this.brush = d3.brushX().extent([[0, 0], [this.w, this.h]]).filter(Base.rightClickOnly)
            .on("end", function () {
                Base.brushendX(self, [self.domain_fix.xMin, self.domain_fix.xMax], 750, SpectrumPlot.update_y, SpectrumPlot.update_peaks);
            });
        this.peakArea.select("#brushArea").call(this.brush);
        // peaks
        this.peakArea.selectAll()
            .data(this.spectrum.peaks)
            .enter()
            .append("rect")
            .attr("y", function (d) {
                return self.y(d.intensity);
            })
//            .style("width", "1px") //do not use attr for the width here, it will not allow to override the values from css!
            .attr("height", function (d) {
                return self.h - self.y(d.intensity);
            })
            .attr("id", function (d, i) {
                return "peak" + i;
            })
            .attr("class", function (d, i) {
                return (self.selected.leftClick === i) ? "peak_select peak" : SpectrumPlot.resetColor(self, d);
            })
            .attr("x", function (d) {
                var rectWidth = this.getBoundingClientRect().width;
                return self.x(d.mz) - rectWidth / 2;
            });
        if (self.selected.leftClick !== -1 && self.selected.leftClick !== null) {
            SpectrumPlot.update_shadow(self, "#shadow_select", self.selected.leftClick, 0);
        }
        //mouse action
        if (!this.spectrum.name.includes("MS1")) {
            this.svg.on("mousemove", Base.mouseMoving(self.spectrum, self.x, self.y, function (i) {
                const newSelected = d3.select("#peak" + i);
                newSelected.classed("peak_hover", true);
                Base.setXandCenterPeak(self, newSelected);
                self.tooltip.style("opacity", 1);
                self.tooltip.html(Base.annotation(self, self.spectrum.peaks[i]));
                SpectrumPlot.update_shadow(self, "#shadow_hover", i, 0);
                const event = window.event;
                self.translateHover(event.clientX, event.clientY);
                if (self.selected.hover !== i) {
                    const lastSelected = d3.select("#peak" + self.selected.hover);
                    if (self.selected.hover !== self.selected.leftClick) {
                        lastSelected.attr("class", SpectrumPlot.resetColor(self, self.spectrum.peaks[self.selected.hover]));
                    } else {
                        lastSelected.classed("peak_hover", false);
                    }
                    self.selected.hover = i;
                    Base.setXandCenterPeak(self, lastSelected);
                }
            }, function () {
                //NOTE: If the distance between 2 peaks is too narrow, mouseleave might be skipped.
                if (self.selected.hover !== null) {
                    const lastSelected = d3.select("#peak" + self.selected.hover);
                    if (!lastSelected.classed("peak_select")) {
                        lastSelected.attr("class", SpectrumPlot.resetColor(self, self.spectrum.peaks[self.selected.hover]));
                    } else {
                        lastSelected.classed("peak_hover", false);
                    }
                    Base.setXandCenterPeak(self, lastSelected);
                    self.selected.hover = null;
                    self.hideHover();
                    self.peakArea.select("#shadow_hover").attr("visibility", "hidden");
                }
            }));
            this.tooltip.on("mousemove", function () {
                self.translateHover(d3.event.clientX, d3.event.clientY);
            });

            d3.select("svg").on("click", function () {
                if (self.selected.hover != null) {
                    SpectrumPlot.mouseup.bind(document.getElementById("peak" + self.selected.hover))(self, self.spectrum.peaks[self.selected.hover], self.selected.hover);
                }
            });
            document.onkeydown = function (e) {
                SpectrumPlot.keyDown(self, e);
            };
            if (this.structureView) SpectrumPlot.showStructure(self, -1);
        } else {
            d3.select("#spectrumView").on("mousemove", Base.mouseMoving(self.spectrum, self.x, self.y, function (i) {
                const node = d3.select("#peak" + i).node();
                Base.mousemoveGeneral.bind(node)(self, self.spectrum.peaks[i], i);
            }, function () {
                Base.mouseleaveGeneral(self);
            }));
            this.tooltip.on("mousemove", function () {
                self.translateHover(d3.event.clientX, d3.event.clientY);
            });
        }
    }
}

class MirrorPlot extends Base {
    constructor(spectrum1, spectrum2, peakMatchesArr1, peakMatchesArr2, viewStyle, mzLabel, svg_str) {
        super();
        this.build();
        d3.selectAll(".peak_shadow").remove(); // Remove unused elements to avoid confusion.
        this.spectrum1 = spectrum1;
        this.spectrum2 = spectrum2;
        this.peakMatches1 = new Map(this.spectrum1.peaks.map((p, i) => [p, peakMatchesArr1?.[i]]));
        this.peakMatches2 = new Map(this.spectrum2.peaks.map((p, i) => [p, peakMatchesArr2?.[i]]));
        this.viewStyle = viewStyle;
        this.mzLabel = mzLabel; // k<=0: no mz label; 0<k<=length: top k mz labels; k>length: show all mz labels
        this.mzs1 = spectrum1.peaks.map(d => d.mz);
        this.mzs2 = spectrum2.peaks.map(d => d.mz);
        this.mzs1Size = this.mzs1.length;
        this.mzs2Size = this.mzs2.length;
        this.new_h = this.h;
        this.domain_fix.xMin = d3.min([d3.min(this.mzs1), d3.min(this.mzs2)]) - 3;
        this.domain_fix.xMax = d3.max([d3.max(this.mzs1), d3.max(this.mzs2)]) + 3;
        if (viewStyle === "difference") {
            this.x_default = {min: d3.min(this.mzs2) - 1, max: d3.max(this.mzs2) + 3};
        } else {
            this.x_default = {min: this.domain_fix.xMin, max: this.domain_fix.xMax};
        }
        this.margin_h = 0;
        this.y1;
        this.y2;
        this.yAxis1;
        this.yAxis2;
        this.diffAra;
        this.mzLabelArea;
        this.svg_str = svg_str;
    }

    static firstNChar(str, num) {
        return (str.length > num) ? str.slice(0, num) : str;
    }

    upOrDown(currentY, callbackUp, callbackDown) {
        (currentY <= this.margin.top + this.h / 2) ? callbackUp() : callbackDown();
    }

    static update_y(self, duration) {
        const mI1 = d3.max(self.spectrum1.peaks.filter(d => self.domain_tmp.xMin <= d.mz && d.mz <= self.domain_tmp.xMax), (d) => d.intensity);
        const mI2 = d3.max(self.spectrum2.peaks.filter(d => self.domain_tmp.xMin <= d.mz && d.mz <= self.domain_tmp.xMax), (d) => d.intensity);
        const maxInt = d3.max([mI1, mI2]);
        if (maxInt !== undefined && maxInt > 0) {
            self.y1.domain([0, maxInt]);
            self.y2.domain([0, maxInt]);
            self.yAxis1.transition().duration(duration).call(d3.axisLeft(self.y1));
            self.yAxis2.transition().duration(duration).call(d3.axisLeft(self.y2));
        }
    }

    static update_peaksX(self, duration) {
        self.peakArea.selectAll(".peak_mirrorTop").transition().duration(duration)
            .attr("x", function (d) {
                var rectWidth = this.getBoundingClientRect().width;
                return self.x(d.mz) - rectWidth / 2;
            })
            .attr("y", function (d) {
                return self.y1(d.intensity);
            })
            .attr("height", function (d) {
                return self.h / 2 - self.y1(d.intensity);
            });
        self.peakArea.selectAll(".peak_matched").transition().duration(duration)
                    .attr("x", function (d) {
                        var rectWidth = this.getBoundingClientRect().width;
                        return self.x(d.mz) - rectWidth / 2;
                    })
                    .attr("y", function (d) {
                        return self.y1(d.intensity);
                    })
                    .attr("height", function (d) {
                        return self.h / 2 - self.y1(d.intensity);
                    });
        self.peakArea.selectAll(".peak_mirrorBottom").transition().duration(duration)
            .attr("x", function (d) {
                var rectWidth = this.getBoundingClientRect().width;
                return self.x(d.mz) - rectWidth / 2;
            })
            .attr("height", function (d) {
                return self.y2(d.intensity) - self.h / 2;
            });
    }

    static update_mzLabels(self, duration) {
        self.mzLabelArea.selectAll(".mzLabel_1").transition().duration(duration).attr("x", function (d) {
            return self.x(d.mz);
        }).attr("y", function (d) {
            return self.y1(d.intensity) - 5;
        });
        self.mzLabelArea.selectAll(".mzLabel_2").transition().duration(duration).attr("x", function (d) {
            return self.x(d.mz);
        }).attr("y", function (d) {
            return (self.y2(d.intensity) + 15 < self.h / 2 + 28) ? self.h / 2 + 28 : self.y2(d.intensity) + 15;
        });
    }

    static update_diffBands(self, duration) {
        self.diffArea.selectAll(".diff_band").transition().duration(duration)
            .attr("x1", function (d) {
                return self.x(d.mz) - 5;
            })
            .attr("x2", function (d) {
                return self.x(d.mz) + 6;
            }).attr("y1", function (d) {
                return self.y1(d.intensity)
            }).attr("y2", function (d) {
                return self.y1(d.intensity)
            });
    }

    static update_rulers(self, duration) {
        function update_ruler(self, mzs, id, duration) {
            let ruler = self.diffArea.select(id);
            const mzsSize = mzs.length;
            const ruler_min = mzs[0];
            const ruler_max = mzs[mzsSize - 1];
            const x_min = self.x.domain()[0];
            const x_max = self.x.domain()[1];
            ruler.select(".horizontal_ruler").transition().duration(duration)
                .attr("x1", function (d) {
                    return (ruler_min < x_min) ? 0 : self.x(ruler_min);
                })
                .attr("x2", function (d) {
                    return (ruler_max > x_max) ? self.w - 20 : self.x(ruler_max);
                }); //w-20 for legend
            ruler.selectAll(".vertical_ruler").transition().duration(duration)
                .attr("x1", function (d) {
                    return self.x(d.mz);
                })
                .attr("x2", function (d) {
                    return self.x(d.mz);
                });
            let i, x0, x1, x_text;
            for (i = 1; i < mzsSize; i++) {
                x0 = mzs[i - 1];
                x1 = mzs[i];
                x_text = (x1 - x0) / 2 + x0;
                ruler.select("#diff_label" + (i - 1)).transition().duration(duration).attr("x", self.x(x_text));
            }
        };
        update_ruler(self, self.mzs1, "#ruler_1", duration);
        update_ruler(self, self.mzs2, "#ruler_2", duration);
    }

    static zoomedY(self, minIntensity, duration, ...callbackUpdates) {
        const scale_tmp_y = d3.event.transform.rescaleY(self.y1);
        const newDomain = d3.axisBottom(scale_tmp_y).scale().domain();
        self.domain_tmp.yMax = (newDomain[1] > 1) ? 1 : (newDomain[1] > minIntensity) ? newDomain[1] : minIntensity;
        self.y1.domain([0, self.domain_tmp.yMax]);
        self.y2.domain([0, self.domain_tmp.yMax]);
        self.yAxis1.transition().duration(duration).call(d3.axisLeft(self.y1));
        self.yAxis2.transition().duration(duration).call(d3.axisLeft(self.y2));
        d3.select("#zoomAreaY").node().__zoom = d3.zoomIdentity;
        callbackUpdates.forEach(function (callback) {
            callback(self, duration);
        });
    }

    static showStructure(self) {
        self.strucArea.html("");
        document.getElementById('str_border').innerHTML = self.svg_str;
        self.strucArea.select('svg').style('height', '100%').style('width', '100%');
    }

    initStructureView() {
        var self = this;
        this.strucArea = d3.select("#container")
            .append('div')
            .attr("id", "structureView")
            .style("height", this.current.h + "px")
            .style("width", this.w / 4 + "px")
            .style("right", this.margin.outerRight + "px")
            .append('div')
            .attr("id", "str_border")
            .style("height", this.w / 4 + "px")
            .style("width", this.w / 4 + "px");
        this.current.w = this.current.w / 4 * 3 - 15;
        this.w = this.current.w - this.margin.left - this.margin.innerRight;
        d3.select("#spectrumView").attr("width", this.current.w + "px");
        d3.select("#xLabel").attr("x", this.w / 2);
        this.svg.select("#clipArea").attr("width", this.w);
    }

    showMzLabel(k) {
        var self = this;
        var thres1 = 0;
        var thres2 = 0;
        if (k !== undefined) {
            if (k < this.mzs1Size) {
                let intensity1 = this.spectrum1.peaks.map(d => d.intensity);
                thres1 = intensity1.sort((a, b) => b - a)[k - 1];
            }
            if (k < this.mzs2Size) {
                let intensity2 = this.spectrum2.peaks.map(d => d.intensity);
                thres2 = intensity2.sort((a, b) => b - a)[k - 1];
            }
        }
        this.mzLabelArea.selectAll()
            .data(this.spectrum1.peaks)
            .enter()
            .append("text")
            .attr("class", "mzLabel_1 mzLabel label spectrum_legend")
            .attr("id", function (d, i) {
                return "mzLabel" + i;
            })
            .attr("x", function (d) {
                return self.x(d.mz);
            })
            .attr("y", function (d) {
                return self.y1(d.intensity) - 5;
            })
            .text(function (d) {
                return (d.intensity >= thres1) ? d.mz.toFixed(self.decimal_place).toString() : "";
            });
        this.mzLabelArea.selectAll()
            .data(this.spectrum2.peaks)
            .enter()
            .append("text")
            .attr("class", "mzLabel_2 mzLabel label spectrum_legend")
            .attr("id", function (d, i) {
                return "mzLabel" + (i + self.mzs1Size);
            })
            .attr("x", function (d) {
                return self.x(d.mz);
            })
            .attr("y", function (d) {
                return (self.y2(d.intensity) + 15 < self.h / 2 + 28) ? self.h / 2 + 28 : self.y2(d.intensity) + 15;
            })
            .text(function (d) {
                return (d.intensity >= thres2) ? d.mz.toFixed(self.decimal_place).toString() : "";
            });
    }

    //difference viewer
    showDifference() {
        var self = this;
        let ruler_1 = this.diffArea.append('g').attr("id", "ruler_1");
        let ruler_2 = this.diffArea.append('g').attr("id", "ruler_2");
        let diff_bands = this.diffArea.append('g').attr("id", "diff_bands");

        // difference ruler: y0=point away from horizontal line, y1=point on the horizontal line
        function plotDiffRuler(self, data, mzs, mzsSize, id, y0, y1) {
            if (mzsSize > 1) {
                let ruler = self.diffArea.select(id);
                const num = id.split('_')[1];
                ruler.selectAll()
                    .data(data)
                    .enter()
                    .append("line")
                    .attr("class", "vertical_ruler diff_ruler")
                    .attr("id", function (d, i) {
                        return "v" + num + "_ruler" + i;
                    })
                    .attr("x1", function (d) {
                        return self.x(d.mz);
                    })
                    .attr("y1", y0)
                    .attr("x2", function (d) {
                        return self.x(d.mz);
                    })
                    .attr("y2", y1);
                ruler.append("line")
                    .attr("class", "horizontal_ruler diff_ruler")
                    .attr("id", "h" + num + "_ruler")
                    .attr("x1", self.x(mzs[0]))
                    .attr("y1", y1)
                    .attr("x2", self.x(mzs[mzsSize - 1]))
                    .attr("y2", y1);
            }
        };

        // difference label
        function plotDiffLabels(self, mzs, mzsSize, id, y) {
            let i, x0, x1, x_text, diff,
                ruler = self.diffArea.select(id);
            for (i = 1; i < mzsSize; i++) {
                x0 = mzs[i - 1];
                x1 = mzs[i];
                x_text = (x1 - x0) / 2 + x0;
                diff = (x1 - x0).toFixed(self.decimal_place);
                ruler.append('text')
                    .attr("class", "label diff_label spectrum_legend")
                    .attr("id", "diff_label" + (i - 1))
                    .attr("x", self.x(x_text))
                    .attr("y", y)
                    .text(diff.toString());
            }
        };
        plotDiffRuler(self, self.spectrum1.peaks, self.mzs1, self.mzs1Size, "#ruler_1", 10, 0);
        plotDiffRuler(self, self.spectrum2.peaks, self.mzs2, self.mzs2Size, "#ruler_2", self.h - 10, self.h);
        plotDiffLabels(self, self.mzs1, self.mzs1Size, "#ruler_1", -5);
        plotDiffLabels(self, self.mzs2, self.mzs2Size, "#ruler_2", self.h + 15);
        //difference band
        diff_bands.selectAll()
            .data(this.spectrum2.peaks)
            .enter()
            .append("line")
            .attr("class", "diff_band")
            .attr("x1", function (d) {
                return self.x(d.mz) - 5;
            })
            .attr("y1", function (d) {
                return self.y1(d.intensity)
            })
            .attr("x2", function (d) {
                return self.x(d.mz) + 6;
            }) // because the peak width is 2px
            .attr("y2", function (d) {
                return self.y1(d.intensity)
            });
    }

    plot() {
        var self = this;
        if (this.svg_str !== null && this.svg_str !== undefined) {
            this.initStructureView();
        }
        // initiation for difference and mass label
        if (this.viewStyle === 'difference') {
            this.new_h = this.h - this.margin.diff_vertical * 2;
            this.margin_h = this.margin.diff_vertical;
            this.svg.append("defs").append("svg:clipPath")
                .attr("id", "diff-clip")
                .append("svg:rect")
                .attr("width", this.w - 20)
                .attr("height", this.h + 30)
                .attr("x", 0)
                .attr("y", -15);
            this.diffArea = d3.select("#content").append('g').attr("id", "differences").attr("clip-path", "url(#diff-clip)");
        }
        if (this.mzLabel > 0) {
            this.new_h = this.h - this.margin.diff_vertical * 2;
            this.margin_h = this.margin.diff_vertical;
            this.mzLabelArea = d3.select("#content").append('g').attr("id", "mzLabels").attr("clip-path", "url(#clip)");
        }
        // X axis
        if (this.domain_tmp.xMin === null || this.domain_tmp.xMax === null) {
            this.x = d3.scaleLinear().range([0, this.w - 20]).domain([this.x_default.min, this.x_default.max]);
            this.domain_tmp.xMin = this.x_default.min;
            this.domain_tmp.xMax = this.x_default.max;
        } else {
            this.x = d3.scaleLinear().range([0, this.w - 20]).domain([this.domain_tmp.xMin, this.domain_tmp.xMax]);
        }
        if (this.viewStyle === "normal") {
            this.xAxis = this.svg.append("g")
                .attr("id", "xAxis")
                .attr("transform", "translate(0," + this.h + ")")
                .call(d3.axisBottom(this.x));
            this.svg.append("g")
                .attr("id", "xAxis_middleline")
                .attr("transform", "translate(0," + this.h / 2 + ")")
                .call(d3.axisBottom(this.x).tickValues([]));
        } else if (this.viewStyle === "simple" || this.viewStyle === 'difference') {
            this.xAxis = this.svg.append("g")
                .attr("id", "xAxis")
                .attr("transform", "translate(0," + this.h / 2 + ")")
                .call(d3.axisBottom(this.x));
        }
        const maxInt = d3.max([d3.max(this.spectrum1.peaks, (d) => d.intensity), d3.max(this.spectrum2.peaks, (d) => d.intensity)]) || 1.0;
        // Y axis 1
        this.y1 = d3.scaleLinear().domain([0, maxInt]).range([this.h / 2, this.margin_h]).nice();
        this.yAxis1 = this.svg.append("g").attr("id", "yAxis1").call(d3.axisLeft(this.y1));
        // Y axis 2
        this.y2 = d3.scaleLinear().domain([0, maxInt]).range([this.h / 2, this.h - this.margin_h]).nice();
        this.yAxis2 = this.svg.append("g").attr("id", "yAxis2").call(d3.axisLeft(this.y2));
        // Y axis zoom area
        this.zoomAreaY = d3.select("#container")
            .append("div")
            .attr("id", "zoomAreaY")
            .style("position", "absolute")
            .style("left", 0 + "px")
            .style("top", this.margin.top + "px")
            .style("width", this.margin.left + "px")
            .style("height", this.h + "px");
        const minI1 = d3.min(this.spectrum1.peaks.map(d => d.intensity));
        const minI2 = d3.min(this.spectrum2.peaks.map(d => d.intensity));
        const minIntensity = d3.min([minI1, minI2]) || 0.1;

        this.svg.selectAll(".label").attr("visibility", "visible");
        // legends: 2 spectrum names
        this.svg.append("text")
            .attr("class", "legend spectrum_legend") // the class spectrum_legend exists only in sirius_frontend
            .attr("x", -this.h / 4)
            .text(MirrorPlot.firstNChar(this.spectrum1["name"], 26));
        this.svg.append("text")
            .attr("class", "legend spectrum_legend") // the class spectrum_legend exists only in sirius_frontend
            .attr("x", -this.h * 3 / 4)
            .text(MirrorPlot.firstNChar(this.spectrum2["name"], 26));
        this.svg.selectAll(".legend")
            .attr("y", this.w)
            .attr("transform", "rotate(-90)");
        this.svg.select("#clipArea").attr("width", this.w - 20);
        // zoom, pan and brush
        var tmp_zoom, tmp_zoom_y, tmp_pan, tmp_brush;
        if (this.viewStyle === "difference" && this.mzLabel > 0) {
            tmp_zoom = function () {
                Base.zoomedX(self, [self.domain_fix.xMin, self.domain_fix.xMax], 250, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_mzLabels, MirrorPlot.update_diffBands);
            };
            tmp_zoom_y =  function () {
                MirrorPlot.zoomedY(self, minIntensity, 250, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_mzLabels, MirrorPlot.update_diffBands);
            };
            tmp_pan = function () {
                var selection = d3.select(this);
                Base.panX(self, selection, [self.domain_fix.xMin, self.domain_fix.xMax], 100, 300, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_mzLabels, MirrorPlot.update_diffBands);
            };
            tmp_brush = function () {
                Base.brushendX(self, [self.x_default.min, self.x_default.max], 750, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_mzLabels, MirrorPlot.update_diffBands);
            };
        } else if (this.viewStyle === "difference" && this.mzLabel <= 0) {
            tmp_zoom = function () {
                Base.zoomedX(self, [self.domain_fix.xMin, self.domain_fix.xMax], 250, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_diffBands);
            };
            tmp_zoom_y =  function () {
                MirrorPlot.zoomedY(self, minIntensity, 250, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_diffBands);
            };
            tmp_pan = function () {
                var selection = d3.select(this);
                Base.panX(self, selection, [self.domain_fix.xMin, self.domain_fix.xMax], 100, 300, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_diffBands);
            };
            tmp_brush = function () {
                Base.brushendX(self, [self.x_default.min, self.x_default.max], 750, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_rulers, MirrorPlot.update_diffBands);
            };
        } else if (this.viewStyle !== "difference" && this.mzLabel > 0) {
            tmp_zoom = function () {
                Base.zoomedX(self, [self.domain_fix.xMin, self.domain_fix.xMax], 250, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_mzLabels);
            };
            tmp_zoom_y =  function () {
                MirrorPlot.zoomedY(self, minIntensity, 250, MirrorPlot.update_peaksX, MirrorPlot.update_mzLabels);
            };
            tmp_pan = function () {
                var selection = d3.select(this);
                Base.panX(self, selection, [self.domain_fix.xMin, self.domain_fix.xMax], 100, 300, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_mzLabels);
            };
            tmp_brush = function () {
                Base.brushendX(self, [self.x_default.min, self.x_default.max], 750, MirrorPlot.update_y, MirrorPlot.update_peaksX, MirrorPlot.update_mzLabels);
            };
        } else {
            tmp_zoom = function () {
                Base.zoomedX(self, [self.domain_fix.xMin, self.domain_fix.xMax], 250, MirrorPlot.update_y, MirrorPlot.update_peaksX);
            };
            tmp_zoom_y =  function () {
                MirrorPlot.zoomedY(self, minIntensity, 250, MirrorPlot.update_peaksX);
            };
            tmp_pan = function () {
                var selection = d3.select(this);
                Base.panX(self, selection, [self.domain_fix.xMin, self.domain_fix.xMax], 100, 300, MirrorPlot.update_y, MirrorPlot.update_peaksX);
            };
            tmp_brush = function () {
                Base.brushendX(self, [self.x_default.min, self.x_default.max], 750, MirrorPlot.update_y, MirrorPlot.update_peaksX);
            }
        }
        this.zoomX = d3.zoom().extent([[0, this.margin_h], [this.w, this.new_h]]).on("zoom", tmp_zoom);
        this.peakArea.select("#brushArea").call(this.zoomX).on("dblclick.zoom", null).on("mousedown.zoom", tmp_pan);
        this.brush = d3.brushX().extent([[0, 0], [this.w - 20, this.h]]).filter(Base.rightClickOnly).on("end", tmp_brush);
        this.peakArea.select("#brushArea").call(this.brush);
        // Y zoom
        this.zoomY = d3.zoom().on("zoom", tmp_zoom_y);
        this.zoomAreaY.call(this.zoomY).on("dblclick.zoom", null);
        // Peaks 1
        this.peakArea.selectAll()
            .data(self.spectrum1.peaks)
            .enter()
            .append("rect")
            .attr("class", function (d) {
                return (self.peakMatches1.get(d) != null) ? "peak_matched peak" : "peak_mirrorTop peak";
            })
            .attr("id", function (d, i) {
                return "peak" + i;
            })
            .attr("x", function (d) {
                return self.x(d.mz);
            })
            .attr("y", function (d) {
                return self.y1(d.intensity);
            })
//            .style("width", "1px") //do not use attr for the width here, it will not allow to override the values from css!
            .attr("height", function (d) {
                return self.h / 2 - self.y1(d.intensity);
            });
        // Peaks 2
        this.peakArea.selectAll()
            .data(self.spectrum2.peaks)
            .enter()
            .append("rect")
            .attr("class", "peak_mirrorBottom peak")
            .attr("id", function (d, i) {
                return "peak" + (i + self.mzs1Size);
            })
            .attr("x", function (d) {
                return self.x(d.mz);
            })
            .attr("y", self.h / 2)
//            .style("width", "1px") //do not use attr for the width here, it will not allow to override the values from css!
            .attr("height", function (d) {
                return self.y2(d.intensity) - self.h / 2;
            });
        // difference and mz labels
        if (this.viewStyle === "difference") this.showDifference();
        if (this.mzLabel > 0) this.showMzLabel(this.mzLabel);
        // structure view
        if (this.svg_str !== null && this.svg_str !== undefined) {
            MirrorPlot.showStructure(self);
        }
        // mouse actions
        d3.select("#spectrumView").on("mousemove", function () {
            let currentY = d3.event.clientY;
            self.upOrDown(currentY,
                Base.mouseMoving(self.spectrum1, self.x, self.y1, function (i) {
                    const node = d3.select("#peak" + i).node();
                    Base.mousemoveGeneral.bind(node)(self, self.spectrum1.peaks[i], i);
                }, function () {
                    Base.mouseleaveGeneral(self);
                }),
                Base.mouseMoving(self.spectrum2, self.x, self.y2, function (i) {
                    const node = d3.select("#peak" + (i + self.mzs1Size)).node();
                    Base.mousemoveGeneral.bind(node)(self, self.spectrum2.peaks[i], (i + self.mzs1Size));
                }, function () {
                    Base.mouseleaveGeneral(self);
                })
            );
        });
        this.tooltip.on("mousemove", function () {
            self.translateHover(d3.event.clientX, d3.event.clientY);
        });
    }
}

class Main {
    constructor() {
        this.svg_str = null;
        this.data;
        this.spectrumPlot;
        this.mirrorStyle = "difference"; // default setting
        this.showMz = 5; // default setting
    }

    clear() {
        d3.select("#container").html("");
        this.spectrumPlot = undefined;
    }

    spectraViewer() {
        var self = this;
        let mirror = false; // only for browser version, which quickly switches mirror plot styles without forgetting structure.
        if (this.data.spectra[1] == null) {
            if (this.svg_str !== null) {
                // MS2 + StructureViewer //todo replace
                this.spectrumPlot = new SpectrumPlot(this.data, this.svg_str, true);
            } else { // MS1 or MS2 without structureViewer
                this.spectrumPlot = new SpectrumPlot(this.data, false);
            }
        } else {
            // MirrorPlot viewStyle: "simple"(+false), "normal"(+false), "difference without mzLabels"(+false), "difference with mzLabels"(+true)
            // current default style: "difference with mzLabels"(+true)
            this.spectrumPlot = new MirrorPlot(this.data.spectra[0], this.data.spectra[1], this.data.peakMatches?.[0], this.data.peakMatches?.[1], this.mirrorStyle, this.showMz, this.svg_str);
            mirror = true; // only for browser version, which quickly switches mirror plot styles without forgetting structure.
        }
        window.addEventListener("resize", function () {
            self.spectrumPlot.build();
            self.spectrumPlot.plot();
            if (self.spectrumPlot.constructor.name === 'SpectrumPlot' && self.spectrumPlot.leftClickSelected !== null) {
                SpectrumPlot.showStructure(self.spectrumPlot, self.spectrumPlot.leftClickSelected);
            }
        });
        this.spectrumPlot.plot();
        if (mirror === false) { // only for browser version, which quickly switches mirror plot styles without forgetting structure.
            this.svg_str = null;
        }
    }

    loadXMLFileFromBrowser() {
        let fileToRead = document.getElementById("XMLfileToLoad").files[0];
        var reader = new FileReader();
        let self = this;
        reader.onload = function() { self.svg_str = reader.result; }
        reader.readAsText(fileToRead);
    }

    loadJSONDataAndStructure(data_spectra, data_svg, mirrorStyle, showMz) {
        d3.select("#debug").text("debug div visible?");
        if (showMz !== null)
            this.showMz = showMz;
        if (mirrorStyle !== null)
            this.mirrorStyle = mirrorStyle;
        if (data_svg !== null)
            this.svg_str = data_svg;
        if ((typeof data_spectra) == "string") {
            this.data = JSON.parse(data_spectra);
        } else {
            this.data = data_spectra;
        }
        this.spectraViewer();
        return true;
    }

    loadJSONDataAndStructureFromBrowser() {
        let fileToRead = document.getElementById("ExampleFileToLoad").files[0];
        var reader = new FileReader();
        let self = this;
        reader.onload = function() {
            let content = JSON.parse(reader.result);
            self.loadJSONDataAndStructure(content.spectrum, content.svg, content.mirrorStyle, content.showMz);
        }
        reader.readAsText(fileToRead);
    }

    mirrorPlotFromBrowser(id) {
        this.mirrorStyle = document.getElementById(id).innerText;
        if (this.data !== undefined) {
            this.spectraViewer();
        } else {
            alert("Error: empty data. Please import the files at first two steps.");
        }
    }

    controlMzLabelFromBrowser() {
        this.showMz = document.getElementById("labelNum").value;
        if (this.data !== undefined) {
            this.spectraViewer();
        } else {
            alert("Error: empty data. Please import the files at first two steps.");
        }
    }

}

var test = new Main();
