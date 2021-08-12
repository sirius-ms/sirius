'use strict';
const TOLERANCE = 40;
// General Variables
var svg, tooltip, peakArea, brush, zoom, idleTimeout, data, w, h, x, y, xAxis, yAxis,
current = {w, h},
domain_fix = {xMin: null, xMax: null},
domain_tmp = {xMin: null, xMax: null, yMax: null}, // need for resize
pan = {mouseupCheck: false, mousemoveCheck: false, tolerance: 10, step: 500},
margin = {top: 20, outerRight: 30, innerRight: 20, bottom: 65, left: 60, diff_vertical: 30},
decimal_place = 4,
// MS2 + structure
strucArea, annoArea, ms2Size, mzs,
selected = {leftClick: null, hover: null},
svg_str = null, basic_structure = null,
anno_str = [],
// Mirror Plot
view = {style: "difference", intensity: true}; // style="simple", "normal" oder "difference"
// Note: The intensity viewer is only switchable under difference style now.
// It will not be showed in simple and normal style, although "intensity=true"


d3.select("body")
    .attr("ondragstart", "return false;")
    .attr("ondrop", "return false;")
    .append("div")
        .attr("id", "container");

window.addEventListener('contextmenu', event => event.preventDefault());

window.addEventListener("resize", function(){
    spectraViewer(data);
    if (selected.leftClick !== null) showStructure(selected.leftClick);
});

document.onkeydown = function(e) {
    if (selected.leftClick !== null) {
        var selectedPeak, new_selected = -1;
        if (e.keyCode === 37 && selected.leftClick !== 0) { // left
            new_selected = selected.leftClick - 1;
            selectedPeak = data.spectra[0].peaks[new_selected];
            while (!("formula" in selectedPeak)) {
                if (new_selected === 0) {
                    new_selected = -1;
                    break;
                }
                new_selected = new_selected - 1;
                selectedPeak = data.spectra[0].peaks[new_selected];
            }
        } else if (e.keyCode === 39 && selected.leftClick !== ms2Size-1) { // right
            new_selected = selected.leftClick + 1;
            selectedPeak = data.spectra[0].peaks[new_selected];
            while (!("formula" in selectedPeak)) {
                if (new_selected === ms2Size-1) break;
                new_selected = new_selected + 1;
                selectedPeak = data.spectra[0].peaks[new_selected];
            }
        }
        if (new_selected !== -1) {
            svg.select("#peak"+selected.leftClick).attr("class", resetColor);
            if (selected.leftClick === selected.hover) {
                svg.select("#peak"+selected.leftClick).classed("peak_hover", true);
            }
            try {
                connector.selectionChanged(mzs[new_selected]);
            } catch (error) {
                null;
            }
            selected.leftClick = new_selected;
            svg.select("#peak"+selected.leftClick).classed("peak_select", true);
            if (selectedPeak.mz <= domain_tmp.xMin) {
                setXdomain(selectedPeak.mz-3, domain_tmp.xMax-(domain_tmp.xMin-selectedPeak.mz)-3);
                update_peaks(50);
            } else if (selectedPeak.mz >= domain_tmp.xMax){
                setXdomain(domain_tmp.xMin+(selectedPeak.mz-domain_tmp.xMax)+3, selectedPeak.mz+3);
                update_peaks(50);
            }
            document.getElementById("anno_leftClick").innerText = annotation(selectedPeak).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
            const d = data.spectra[0].peaks[selected.leftClick];
            if ("structureInformation" in d) {
                showStructure(selected.leftClick);
            } else {
                showStructure(-1);
            }
            
        }
    }
};

function resize() {
    current = {w: window.innerWidth, h: window.innerHeight};
    w = current.w - margin.left - margin.outerRight;
    h = current.h - margin.top - margin.bottom;
};

// For functions outside the js script to clear SVG (e.g. when the input is deleted)
function clear() {
    d3.select("#container").html("");
    data = null;
    domain_fix = {xMin: null, xMax: null};
    domain_tmp = {xMin: null, xMax: null, yMax: null};
    pan.mouseupCheck = false;
    pan.mousemoveCheck = false;
    selected = {leftClick: null, hover: null};
    svg_str = null;
    basic_structure = null;
    anno_str = [];
};

function setSelection(mz) {
    if (svg_str !== null) {
        var i;
        for (i in mzs) { if (Math.abs(mzs[i]-mz) < 1e-3) break; }
        const d = data.spectra[0].peaks[i];
        i = Number(i);
        if (selected.leftClick !== i) {
            if (!("formula" in d) && selected.leftClick !== null) cancelSelection(d3.select("#peak"+selected.leftClick));
            if ("formula" in d || i === ms2Size-1) {
                selectNewPeak(d, i, d3.select("#peak"+i));
                if (mz <= self.domain_tmp.xMin) {
                    Base.setXdomain(self, mz-3, self.domain_tmp.xMax-(self.domain_tmp.xMin-mz)-3);
                    SpectrumPlot.update_peaks(self, 50);
                } else if (mz >= self.domain_tmp.xMax){
                    Base.setXdomain(self, self.domain_tmp.xMin+(mz-self.domain_tmp.xMax)+3, mz+3);
                    SpectrumPlot.update_peaks(self, 50);
                }
            }
        }
    }
};

function showStructure(i) {
    strucArea.html("");
    document.getElementById('str_border').innerHTML = svg_str;
    basic_structure = strucArea.select('svg').style('height', '100%').style('width', '100%');
    removeElementStyle("bond", "stroke");
    removeElementStyle("atom", "fill");
    if (i === -1) {
        strucArea.selectAll(".bond").classed("default_bond", true);
        strucArea.selectAll(".atom").classed("default_atom", true);
        basic_structure.style("opacity", 1);
    } else if (i === ms2Size - 1) {
        strucArea.selectAll(".bond").classed("highlight_bond", true);
        strucArea.selectAll(".atom").classed("highlight_atom", true);
        basic_structure.style("opacity", 1);
    } else if ("structureInformation" in data.spectra[0].peaks[i]) {
        const highlight = data.spectra[0].peaks[i].structureInformation;
        highlighting(highlight.bonds, highlight.cuts, highlight.atoms);
        basic_structure.style("opacity", 1);
    } else { // Just for error: it can't be other value
        basic_structure.style("opacity", 0);
    }
};

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

function highlighting(bonds, cuts, atoms) {
    let totalBond = d3.selectAll(".bond").size();
    let total = new Set(Array.from({length: totalBond}, (v, i) => i));
    let bond_set = new Set(bonds);
    let cut_set = new Set(cuts);
    let rest = new Set([...total].filter(x => !bond_set.has(x)));
    rest = new Set([...rest].filter(x => !cut_set.has(x)));
    for (let i in bonds) {
        strucArea.select("#mol1bnd"+(bonds[i]+1)).attr("class", "bond highlight_bond");
    }
    for (let j in cuts) {
        strucArea.select("#mol1bnd"+(cuts[j]+1)).attr("class", "bond highlight_cut");
    }
    for (let l of rest) {
        strucArea.select("#mol1bnd"+(l+1)).attr("class", "bond rest_bond");
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

// Only used in spectrumPlot: peak_1 = unannotated, peak_2 = annotate
function resetColor(d) {
    let precursor = data.spectra[0].peaks[ms2Size-1].formula;
    if (data.spectra[0].name.includes("MS1")) {
        return (Object.keys(d.peakMatches).length !== 0) ? "peak_matched peak" : "peak_1 peak";
    } else {
        if ("structureInformation" in d || (d.formula === precursor && svg_str !== null)) {
            return "peak_2 peak_structInfo peak";
        } else {
            return ("formula" in d) ? "peak_2 peak" : "peak_1 peak";
        }
    }
};

function translateHover(mouse_w, mouse_h) {
    // NOTE: These distances might need to be changed, when the font size and content of hover are changed.
    // current hover style: padding = 5px, border-width = 1px
    let tmp_tooltip = document.querySelector("#tooltip");
    let tmp_h = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("height")) + 12;
    let tmp_w = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("width")) + 12;
    const distanceVertical = 30, distanceHorizontal = 30;
    // style: vertical distance between cursor and hover (depending on size of cursor)
    if (mouse_h+tmp_h+distanceVertical > current.h) {
        tooltip.style("top", (mouse_h - distanceVertical - tmp_h/2 + "px"));
    } else {
        tooltip.style("top", (mouse_h - distanceVertical + "px"));
    }
    // style: horizontal distance between cursor and hover (depending on size of cursor)
    if (mouse_w+tmp_w+distanceHorizontal > current.w) {
        tooltip.style("left", (mouse_w - distanceHorizontal - tmp_w + "px"));
    } else {
        tooltip.style("left", (mouse_w + distanceHorizontal + "px"));
    }
};

function hideHover() {
    tooltip.style("opacity", 0);
    tooltip.html("");
};

function annotation(d) {
    let anno = "";
    if ("formula" in d) {
        let sign = (d.massDeviationMz > 0) ? "+" : "";
        anno = "Formula: " + d.formula.replace(" + [M", "").replace("]","") +
               "<br>Intensity: " + d.intensity.toFixed(decimal_place) +
               "<br>m/z: " + d.mz.toFixed(decimal_place);
        if ("massDeviationMz" in d) {
            anno = anno + "<br>Mass deviation: " + sign + (d.massDeviationMz*1000).toFixed(decimal_place) + " mDa<br>" +
                   "&nbsp;".repeat(25) + "(" + sign + d.massDeviationPpm.toFixed(decimal_place) + " ppm)";
        }
        if ("structureInformation" in d) {
            anno = anno + "<br>Fragmenter Score: " + d.structureInformation.score.toFixed(2);
        }
    } else {
        anno = "m/z: " + d.mz.toFixed(decimal_place) + "<br>Intensity: " + d.intensity.toFixed(decimal_place);
    }
    return anno;
};

function selectNewPeak(d, i, newPeak) {
    if (selected.leftClick !== -1 && selected.leftClick !== null && !newPeak.classed("peak_select")) {
        d3.select("#peak"+selected.leftClick).attr("class", resetColor);
    }
    try {
        connector.selectionChanged(mzs[i]);
    } catch (error) {
        null;
    }
    selected.leftClick = i;
    newPeak.classed("peak_select", true);
    annoArea.attr("id", "anno_leftClick");
    document.getElementById("anno_leftClick").innerText = annotation(d).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
    if ("structureInformation" in d) {
        showStructure(i);
    } else {
        showStructure(-1);
    }   
    hideHover();
};

function cancelSelection(peak) {
    try {
        connector.selectionChanged(-1);
    } catch (error) {
        null;
    }
    selected.leftClick = null;
    peak.classed("peak_select", false);
    document.getElementById("anno_leftClick").innerText = "Left click to choose a purple or green peak...";
    annoArea.attr("id", "nothing");
    showStructure(-1);
};

/*
takes the spectrum and the x-domain and returns a mouse event listener which
calls the given callback with the peak index as argument whenever the
mouse comes close to a peak
*/
const mouseMovingFunction = function(spectrum, tolerance, xdomain, ydomain, callbackIn, callbackLeave) {
    const mzvalues = spectrum.peaks.map(d => d.mz);
    var lastPeakSelected = -1;
    return function() {
        event = d3.event; // -_- old d3 version
        event.preventDefault();
        const mousePointer = d3.mouse(document.getElementById("peaks"));
        const mzcurrent = xdomain.invert(mousePointer[0]-tolerance);
        let closestPeakIndex = -1;
        let bestDist=Infinity;
        for (let k=Math.max(0,d3.bisect(mzvalues, mzcurrent)-1); k < mzvalues.length; ++k) {
            const mzp = xdomain(mzvalues[k]);
            if (mzp > mousePointer[0]+tolerance) {
                break;
            }
            if (mzp >= mousePointer[0]-tolerance) {
                const intens = ydomain(spectrum.peaks[k].intensity/2.0);
                const distance = Math.abs(mzp-mousePointer[0]) + (Math.abs(intens-mousePointer[1]))/2.0;
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
            if (selection>=0) {
                callbackIn(selection);
            } else {
                callbackLeave();
            }
        }
    };
};

var mousemoveGeneral = function(d, i) {
    let event = window.event;
    translateHover(event.clientX, event.clientY);
    tooltip.html(annotation(d));
    tooltip.style("opacity", 1);
    d3.select("#peak"+i).classed("peak_hover", true);
    if (selected.hover !== i) {
        d3.select("#peak"+selected.hover).classed("peak_hover", false);
        selected.hover = i;
    }
};

var mouseleaveGeneral = function() {
    if (selected.hover !== null) {
        d3.select("#peak"+selected.hover).classed("peak_hover", false);
        selected.hover = null;
        hideHover();
    }
};

var mouseup = function(d, i) {
    if (!pan.mousemoveCheck) {
        if ("formula" in d || i === ms2Size-1) {
            let tmp = d3.select("#peak"+i);
            if (selected.leftClick !== null && tmp.classed("peak_select")) {
            	cancelSelection(tmp);
            } else {
                selectNewPeak(d, i, tmp);
            }
        }
    }
    pan.mouseupCheck = false;
    pan.mousemoveCheck = false;
};

function zoomedX(xdomain_fix, duration, ...callbackUpdates) {
    const scale_tmp_x = d3.event.transform.rescaleX(x);
    const newDomain = d3.axisBottom(scale_tmp_x).scale().domain();
    domain_tmp.xMin = (newDomain[0] < xdomain_fix[0]) ? xdomain_fix[0] : newDomain[0];
    domain_tmp.xMax = (newDomain[1] > xdomain_fix[1]) ? xdomain_fix[1] : newDomain[1];
    x.domain([domain_tmp.xMin, domain_tmp.xMax])
    xAxis.transition().duration(duration).call(d3.axisBottom(x));
    peakArea.select("#brushArea").node().__zoom = d3.zoomIdentity;
    callbackUpdates.forEach(function(callback) { callback(duration); });
};

function zoomedY(minIntensity, duration, ...callbackUpdates) {
    const scale_tmp_y = d3.event.transform.rescaleY(y);
    const newDomain = d3.axisBottom(scale_tmp_y).scale().domain();
    domain_tmp.yMax = (newDomain[1] > 1) ? 1 : (newDomain[1] > minIntensity) ? newDomain[1] : minIntensity;
    y.domain([0, domain_tmp.yMax])
    yAxis.transition().duration(duration).call(d3.axisLeft(y));
    d3.select("#zoomAreaY").node().__zoom = d3.zoomIdentity;
    callbackUpdates.forEach(function(callback) { callback(duration); });
};

var update_peaks = function(duration) {
    peakArea.selectAll(".peak").transition().duration(duration)
        .attr("x", function(d) { return x(d.mz)})
        .attr("y", function(d) { return (y(d.intensity) < 0) ? 0 : y(d.intensity); })
        .attr("height", function(d) { return (h-y(d.intensity)>=h) ? h : h-y(d.intensity); });
};

function setXdomain(newXmin, newXmax, duration) {
    x.domain([newXmin, newXmax])
    domain_tmp.xMin = newXmin;
    domain_tmp.xMax = newXmax;
    xAxis.transition().duration(duration).call(d3.axisBottom(x));
};

function panX(selection, xdomain_fix, duration, ...callbackUpdates) {
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
                pan.mouseupCheck = true;
                x0 = d3.event.clientX;
            }
        };
        function mousemovePan() {
            if (pan.mouseupCheck) {
                x1 = d3.event.clientX;
                d = x1 - x0;
                if (Math.abs(d)>=pan.tolerance) {
                    pan.mousemoveCheck = true;
                    newXmin = domain_tmp.xMin-d*(domain_tmp.xMax-domain_tmp.xMin)/pan.step;
                    newXmax = domain_tmp.xMax-d*(domain_tmp.xMax-domain_tmp.xMin)/pan.step;
                    if (newXmin >= xdomain_fix[0] && newXmax <= xdomain_fix[1]) {
                        setXdomain(newXmin, newXmax, duration);
                        callbackUpdates.forEach(function(callback) { callback(duration); });
                    }
                    x0 = x1;
                    d = 0;
                }
            }
        };
        function mouseupPan() {
            if (pan.mouseupCheck && pan.mousemoveCheck) w.on("mousedown", null).on("mousemove", null).on("mouseup", null);
        };
    }
};

function rightClickOnly() { return d3.event.button === 2; };

function brushendX(xdomain_fix, duration, ...callbackUpdates) {
    let extent = d3.event.selection;
    if(!extent){
        if (!idleTimeout) return idleTimeout = setTimeout(function(){ idleTimeout=null; }, 350);
        x.domain([xdomain_fix[0], xdomain_fix[1]])
        domain_tmp.xMin = xdomain_fix[0];
        domain_tmp.xMax = xdomain_fix[1];
        if (y !== undefined) { //temporarily only reset in spectrumPlot
            y.domain([0, 1])
            domain_tmp.yMax = 1;
            yAxis.transition().duration(duration).call(d3.axisLeft(y));
        }
    } else {
        domain_tmp.xMin = x.invert(extent[0]);
        domain_tmp.xMax = x.invert(extent[1]);
        x.domain([domain_tmp.xMin, domain_tmp.xMax])
        peakArea.select("#brushArea").call(brush.move, null);
    }
    xAxis.transition().duration(duration).call(d3.axisBottom(x));
    callbackUpdates.forEach(function(callback) { callback(duration); });
};

function init() {
    d3.select("#container").html("");
    resize();
    svg = d3.select("#container")
        .append('svg')
        .attr("id", "spectrumView")
        .attr("height", current.h)
        .attr("width", current.w)
        .append("g")
            .attr("id", "content")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    // X label
    svg.append("text")
        .attr("class", "label spectrum_label")
        .attr("id", "xLabel")
        .attr("x", w/2)
        .attr("y", h + margin.top + 20)
        .text("m/z");
    // Y label
    svg.append("text")
        .attr("class", "label spectrum_label")
        .attr("id", "yLabel")
        .attr("transform", "rotate(-90)")
        .attr("y", -40)
        .attr("x", -h/2)
        .text("Relative Intensity");

    svg.selectAll(".label").attr("visibility", "hidden");
    //tooltip
    tooltip = d3.select("#container")
        .append("div")
        .attr("id", "tooltip")
        .style("opacity", 0)
    //clipPath & brushing
    svg.append("defs").append("svg:clipPath")
        .attr("id", "clip")
        .append("svg:rect")
        .attr("id", "clipArea")
        .attr("width", w )
        .attr("height", h )
        .attr("x", 0)
        .attr("y", 0);

    peakArea = svg.append("g")
        .attr("id", "peaks")
        .attr("clip-path", "url(#clip)");
    peakArea.append("g")
        .attr("id", "brushArea");
};

function spectrumPlot(spectrum, structureView) {
    function injectStructureInformation(spectrum) {
        const structure = anno_str; // this should not be a global variable
        const formula2nodes = {};
        for (let peak of spectrum.peaks) {
            let formula = peak.formula;
            if (formula) {
                formula = formula.split(" ")[0];
                if (!formula2nodes[formula]) formula2nodes[formula]=[];
                formula2nodes[formula].push(peak);
            }
        }
        for (let struct of structure) {
            const formula = struct.formula;
            if (formula2nodes[formula]) {
                for (let node of formula2nodes[formula]) {
                    node.structureInformation = struct;
                }
            }
        }
    }

    function initStructureView() {
        strucArea = d3.select("#container")
            .append('div')
            .attr("id", "structureView")
            .style("height", current.h+"px")
            .style("width", w/4+"px")
            .style("right", margin.outerRight+"px")
                .append('div')
                .attr("id", "str_border")
                .style("height", w/4+"px")
                .style("width", w/4+"px");
        let anno_top = w/4+10; // because padding=5
        annoArea = d3.select("#structureView")
            .append('p')
            .attr("class", "anno")
            .attr("id", function() { return (selected.leftClick !== null) ? "anno_leftClick" : "nothing"; })
            .style("top", anno_top+"px")
            .text(function() {
                if (selected.leftClick !== null) {
                    return annotation(data.spectra[0].peaks[selected.leftClick]).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
                } else {
                    return "Left click to choose a purple or green peak...";
                }});
        current.w = current.w/4*3 - 15;
        w = current.w - margin.left - margin.innerRight;
        d3.select("#spectrumView").attr("width", current.w+"px");
        d3.select("#xLabel").attr("x", w/2);
        svg.select("#clipArea").attr("width", w);
    };

    mzs = spectrum.peaks.map(d => d.mz);
    ms2Size = mzs.length;
    if (structureView) {
        initStructureView();
        injectStructureInformation(spectrum);
    }
    domain_fix.xMin = d3.min(mzs)-3;
    domain_fix.xMax = d3.max(mzs)+3;
    // X axis
    if (domain_tmp.xMin === null || domain_tmp.xMax === null) {
        x = d3.scaleLinear().range([0, w]).domain([domain_fix.xMin, domain_fix.xMax]);
        domain_tmp.xMin = domain_fix.xMin;
        domain_tmp.xMax = domain_fix.xMax;
    } else {
        x = d3.scaleLinear().range([0, w]).domain([domain_tmp.xMin, domain_tmp.xMax]);
    }
    xAxis = svg.append("g")
        .attr("id", "xAxis")
        .attr("transform", "translate(0," + h + ")")
        .call(d3.axisBottom(x));
    // Y axis
    if (domain_tmp.yMax === null) {
        y = d3.scaleLinear().domain([0, 1]).range([h, 0]);
        domain_tmp.yMax = 1;
    } else {
        y = d3.scaleLinear().domain([0, domain_tmp.yMax]).range([h, 0]);
    }
    yAxis = svg.append("g").attr("id", "yAxis").call(d3.axisLeft(y));
    svg.selectAll(".label").attr("visibility", "visible");
    // zoom and pan
    zoom = d3.zoom().extent([[0,0],[w,h]]).on("zoom", function() { zoomedX([0, domain_fix.xMax], 100, update_peaks); });
    var zoomAreaY = d3.select("#container")
        .append("div")
        .attr("id", "zoomAreaY")
        .style("position", "absolute")
        .style("left", 0+"px")
        .style("top", margin.top+"px")
        .style("width", margin.left+"px")
        .style("height", h+"px");
    const minIntensity = d3.min(spectrum.peaks.map(d => d.intensity));
    var zoomY = d3.zoom().on("zoom", function() { zoomedY(minIntensity, 100, update_peaks); });
    zoomAreaY.call(zoomY).on("dblclick.zoom", null);
    peakArea.select("#brushArea").call(zoom)
        .on("dblclick.zoom", null)
        .on("mousedown.zoom", function() {
            var selection = d3.select(this);
            panX(selection, [0, domain_fix.xMax], 50, update_peaks);
        });
    // brush
    brush = d3.brushX().extent( [ [0,0], [w,h] ]).filter(rightClickOnly)
        .on("end", function() { brushendX([domain_fix.xMin, domain_fix.xMax],750, update_peaks);} );
    peakArea.select("#brushArea").call(brush);
    // peaks
    peakArea.selectAll()
        .data(spectrum.peaks)
        .enter()
        .append("rect")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("height", function(d) { return h - y(d.intensity); })
            .attr("id", function(d, i) { return "peak"+i; })
            .attr("class", function(d, i) { return (selected.leftClick === i) ? "peak_select peak" : resetColor(d); });
    // mouse actions
    if (structureView) {
        svg.on("mousemove", mouseMovingFunction(spectrum, TOLERANCE, x, y, function(i) {
            const newSelected = d3.select("#peak"+i);
            if (!newSelected.classed("peak_select")) {
                newSelected.attr("class", "peak_hover peak");
            } else {
                newSelected.attr("class", "peak_hover peak_select peak");
            }
            tooltip.style("opacity", 1);
            tooltip.html(annotation(spectrum.peaks[i]));
            const event = window.event;
            translateHover(event.clientX, event.clientY);
            if (selected.hover !== i) {
                const lastSelected = d3.select("#peak"+selected.hover);
                if (selected.hover !== selected.leftClick) {
                    lastSelected.attr("class", resetColor);
                } else {
                    lastSelected.classed("peak_hover", false);
                }
                selected.hover = i;
            }
        }, function() {
        //NOTE: If the distance between 2 peaks is too narrow, mouseleave might be skipped.
            if (selected.hover !== null) {
                const lastSelected = d3.select("#peak"+selected.hover);
                if (!lastSelected.classed("peak_select")) {
                    lastSelected.attr("class", resetColor);
                } else {
                    lastSelected.classed("peak_hover", false);
                }
                selected.hover = null;
                hideHover();
            }
        }));
        tooltip.on("mousemove", function(){ translateHover(d3.event.clientX, d3.event.clientY); });

        d3.select("svg").on("click", function(){
            if (selected.hover != null) {
                mouseup.bind(document.getElementById("peak"+selected.hover))(spectrum.peaks[selected.hover], selected.hover);
            }
        });
        showStructure(-1);
    } else {
        svg.on("mousemove", mouseMovingFunction(spectrum, TOLERANCE, x, y, function(i) {
            const node = d3.select("#peak"+i).node();
            mousemoveGeneral.bind(node)(spectrum.peaks[i],i);
        }, function() {
            mouseleaveGeneral();
        })) ;
        tooltip.on("mousemove", function(){ translateHover(d3.event.clientX, d3.event.clientY); });
    }
};

function mirrorPlot(spectrum1, spectrum2, viewStyle, intensityViewer) {
    function firstNChar(str, num) { return (str.length > num) ? str.slice(0, num) : str; };

    function upOrDown(currentY, callbackUp, callbackDown) { (currentY <= margin.top+h/2) ? callbackUp() : callbackDown(); };

    var update_peaksX = function(duration) {peakArea.selectAll(".peak").transition().duration(duration).attr("x", function(d) { return x(d.mz); }); };

    var update_intensities = function(duration) {
        intensityArea.selectAll(".intensity").transition().duration(duration).attr("x", function(d) { return x(d.mz); });
    };
    var update_diffBands = function(duration) {
        diffArea.selectAll(".diff_band").transition().duration(duration)
            .attr("x1", function(d) { return x(d.mz)-5; })
            .attr("x2", function(d) { return x(d.mz)+6; });
    };
    var update_rulers = function(duration) {
        function update_ruler(mzs, id, duration) {
            let ruler = diffArea.select(id);
            const mzsSize = mzs.length;
            const ruler_min = mzs[0];
            const ruler_max = mzs[mzsSize-1];
            const x_min = x.domain()[0];
            const x_max = x.domain()[1];
            ruler.select(".horizontal_ruler").transition().duration(duration)
                .attr("x1", function(d) { return (ruler_min<x_min) ? 0 : x(ruler_min); })
                .attr("x2", function(d) { return (ruler_max>x_max) ? w-20 : x(ruler_max); }); //w-20 for legend
            ruler.selectAll(".vertical_ruler").transition().duration(duration)
                .attr("x1", function(d) { return x(d.mz); })
                .attr("x2", function(d) { return x(d.mz); });
            let i, x0, x1, x_text;
            for (i = 1; i < mzsSize; i++) {
                x0 = mzs[i-1];
                x1 = mzs[i];
                x_text = (x1-x0)/2+x0;
                ruler.select("#diff_label"+(i-1)).transition().duration(duration).attr("x", x(x_text));
            }
        };
        update_ruler(mzs1, "#ruler_1", duration);
        update_ruler(mzs2, "#ruler_2", duration);
    };
    //intensity viewer, maybe also available in spectrumViewer in the future
    function showIntensity() {
        intensityArea.selectAll()
            .data(spectrum1.peaks)
            .enter()
            .append("text")
                .attr("class", "intensity_1 intensity label spectrum_legend")
                .attr("id", function(d, i) { return "intensity"+i; })
                .attr("x", function(d) { return x(d.mz); })
                .attr("y", function(d) { return y1(d.intensity)-5; })
                .text(function(d) { return d.mz.toFixed(decimal_place).toString(); });
        intensityArea.selectAll()
            .data(spectrum2.peaks)
            .enter()
            .append("text")
                .attr("class", "intensity_2 intensity label spectrum_legend")
                .attr("id", function(d, i) { return "intensity"+(i+mzs1Size); })
                .attr("x", function(d) { return x(d.mz); })
                .attr("y", function(d) { return (y2(d.intensity)+15<h/2+28) ? h/2+28 : y2(d.intensity)+15; })
                .text(function(d) { return d.mz.toFixed(decimal_place).toString(); });
    };
    //difference viewer
    function showDifference() {
        let ruler_1 = diffArea.append('g').attr("id", "ruler_1");
        let ruler_2 = diffArea.append('g').attr("id", "ruler_2");
        let diff_bands = diffArea.append('g').attr("id", "diff_bands");
        // difference ruler: y0=point away from horizontal line, y1=point on the horizontal line
        function plotDiffRuler(data, mzs, mzsSize, id, y0, y1) {
            let ruler = diffArea.select(id);
            const num = id.split('_')[1];
            ruler.selectAll()
                .data(data)
                .enter()
                .append("line")
                    .attr("class", "vertical_ruler diff_ruler")
                    .attr("id", function(d, i) { return "v"+num+"_ruler"+i; })
                    .attr("x1", function(d) { return x(d.mz); })
                    .attr("y1", y0)
                    .attr("x2", function(d) { return x(d.mz); })
                    .attr("y2", y1);
            ruler.append("line")
                    .attr("class", "horizontal_ruler diff_ruler")
                    .attr("id", "h"+num+"_ruler")
                    .attr("x1", x(mzs[0]))
                    .attr("y1", y1)
                    .attr("x2", x(mzs[mzsSize-1]))
                    .attr("y2", y1);
        };
        // difference label
        function plotDiffLabels(mzs, mzsSize, id, y) {
            let i, x0, x1, x_text, diff,
            ruler = diffArea.select(id);
            for (i = 1; i < mzsSize; i++) {
                x0 = mzs[i-1];
                x1 = mzs[i];
                x_text = (x1-x0)/2+x0;
                diff = (x1-x0).toFixed(decimal_place);
                ruler.append('text')
                    .attr("class", "label diff_label spectrum_legend")
                    .attr("id", "diff_label"+(i-1))
                    .attr("x", x(x_text))
                    .attr("y", y)
                    .text(diff.toString());
            }
        };
        plotDiffRuler(spectrum1.peaks, mzs1, mzs1Size, "#ruler_1", 10, 0);
        plotDiffRuler(spectrum2.peaks, mzs2, mzs2Size, "#ruler_2", h-10, h);
        plotDiffLabels(mzs1, mzs1Size, "#ruler_1", -5);
        plotDiffLabels(mzs2, mzs2Size, "#ruler_2", h+15);
        //difference band
        diff_bands.selectAll()
            .data(spectrum2.peaks)
            .enter()
            .append("line")
                .attr("class", "diff_band")
                .attr("x1", function(d) { return x(d.mz)-5; })
                .attr("y1", function(d) { return y1(d.intensity) })
                .attr("x2", function(d) { return x(d.mz)+6; }) // because the peak width is 2px
                .attr("y2", function(d) { return y1(d.intensity) });
    };
    // begin to plot...
    const mzs1 = spectrum1.peaks.map(d => d.mz);
    const mzs2 = spectrum2.peaks.map(d => d.mz);
    const mzs1Size = mzs1.length;
    const mzs2Size = mzs2.length;
    const x_default = {min: d3.min(mzs2)-1, max: d3.max(mzs2)+1};
    var y1, y2, diffArea, intensityArea,
    margin_h = 0,
    new_h = h;

    domain_fix.xMin = d3.min([d3.min(mzs1), d3.min(mzs2)])-1;
    domain_fix.xMax = d3.max([d3.max(mzs1), d3.max(mzs2)])+1;
    // difference and intensity viewer initiation
    if (viewStyle === 'difference') {
        new_h = h - margin.diff_vertical*2;
        margin_h = margin.diff_vertical;
        svg.append("defs").append("svg:clipPath")
            .attr("id", "diff-clip")
            .append("svg:rect")
            .attr("width", w-20 )
            .attr("height", h+30 )
            .attr("x", 0)
            .attr("y", -15);
        diffArea = d3.select("#content").append('g').attr("id", "differences").attr("clip-path", "url(#diff-clip)");
        if (intensityViewer) intensityArea = d3.select("#content").append('g').attr("id", "intensities").attr("clip-path", "url(#clip)");
    }
    // X axis
    if (domain_tmp.xMin === null || domain_tmp.xMax === null) {
        x = d3.scaleLinear().range([0, w-20]).domain([x_default.min, x_default.max]);
        domain_tmp.xMin = x_default.min;
        domain_tmp.xMax = x_default.max;
    } else {
        x = d3.scaleLinear().range([0, w-20]).domain([domain_tmp.xMin, domain_tmp.xMax]);
    }
    if (viewStyle === "normal") {
        xAxis = svg.append("g")
            .attr("id", "xAxis")
            .attr("transform", "translate(0," + h + ")")
            .call(d3.axisBottom(x));
        svg.append("g")
            .attr("id", "xAxis_middleline")
            .attr("transform", "translate(0," + h/2 + ")")
            .call(d3.axisBottom(x).tickValues([]));
    } else if (viewStyle === "simple" || viewStyle === 'difference') {
        xAxis = svg.append("g")
            .attr("id", "xAxis")
            .attr("transform", "translate(0," + h/2 + ")")
            .call(d3.axisBottom(x));
    }
    // Y axis 1
    y1 = d3.scaleLinear().domain([0, 1]).range([h/2, margin_h])
    svg.append("g").attr("id", "yAxis1").call(d3.axisLeft(y1));
    // Y axis 2
    y2 = d3.scaleLinear().domain([0, 1]).range([h/2, h-margin_h])
    svg.append("g").attr("id", "yAxis2").call(d3.axisLeft(y2));
    svg.selectAll(".label").attr("visibility", "visible");
    // legends: 2 spectrum names
    svg.append("text")
        .attr("class", "legend spectrum_legend")
        .attr("x", -h/4)
        .text(firstNChar(spectrum1["name"], 20));
    svg.append("text")
        .attr("class", "legend spectrum_legend")
        .attr("x", -h*3/4)
        .text(firstNChar(spectrum2["name"], 20));
    svg.selectAll(".legend")
        .attr("y", w)
        .attr("transform", "rotate(-90)");
    svg.select("#clipArea").attr("width", w-20);
    // zoom, pan and brush
    var tmp_zoom, tmp_pan, tmp_brush;
    if (viewStyle === "difference" && intensityViewer) {
        tmp_zoom = function() { zoomedX([domain_fix.xMin, domain_fix.xMax], 250, update_peaksX, update_rulers, update_intensities, update_diffBands); };
        tmp_pan = function() {
            var selection = d3.select(this);
            panX(selection, [domain_fix.xMin, domain_fix.xMax], 150, update_peaksX, update_rulers, update_intensities, update_diffBands);
        };
        tmp_brush = function() { brushendX([x_default.min, x_default.max], 750, update_peaksX, update_rulers, update_intensities, update_diffBands); };
    } else if (viewStyle === "difference" && !intensityViewer) {
        tmp_zoom = function() { zoomedX([domain_fix.xMin, domain_fix.xMax], 250, update_peaksX, update_rulers, update_diffBands); };
        tmp_pan = function() {
            var selection = d3.select(this);
            panX(selection, [domain_fix.xMin, domain_fix.xMax], 150, update_peaksX, update_rulers, update_diffBands);
        };
        tmp_brush = function() { brushendX([x_default.min, x_default.max], 750, update_peaksX, update_rulers, update_diffBands); };
    } else {
        tmp_zoom = function() { zoomedX([domain_fix.xMin, domain_fix.xMax], 250, update_peaksX); };
        tmp_pan = function() {
            var selection = d3.select(this);
            panX(selection, [domain_fix.xMin, domain_fix.xMax], 150, update_peaksX);
        };
        tmp_brush = function() { brushendX([x_default.min, x_default.max], 750, update_peaksX); };
    }
    zoom = d3.zoom().extent([[0,margin_h],[w,new_h]]).on("zoom", tmp_zoom);
    peakArea.select("#brushArea").call(zoom).on("dblclick.zoom", null).on("mousedown.zoom", tmp_pan);
    brush = d3.brushX().extent( [ [0,0], [w-20, h] ]).filter(rightClickOnly).on("end", tmp_brush);
    peakArea.select("#brushArea").call(brush);
    // Peaks 1
    peakArea.selectAll()
        .data(spectrum1.peaks)
        .enter()
        .append("rect")
            .attr("class", function(d) {return (Object.keys(d.peakMatches).length !== 0) ? "peak_matched peak" : "peak_1 peak";})
            .attr("id", function(d, i) { return "peak"+i; })
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); });
    // Peaks 2
    peakArea.selectAll()
        .data(spectrum2.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak_2 peak")
            .attr("id", function(d, i) { return "peak"+(i+mzs1Size); })
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity)-h/2; });
    // difference and intensity viewer
    if (viewStyle === "difference") {
        showDifference();
        if (intensityViewer) showIntensity();
    }
    // mouse actions
    svg.on("mousemove", function() {
        let currentY = d3.event.clientY;
        upOrDown(currentY,
            mouseMovingFunction(spectrum1, TOLERANCE, x, y1, function(i) {
                const node = d3.select("#peak"+i).node();
                mousemoveGeneral.bind(node)(spectrum1.peaks[i],i);
            }, function() {
                mouseleaveGeneral();
            }),
            mouseMovingFunction(spectrum2, TOLERANCE, x, y2, function(i) {
                const node = d3.select("#peak"+(i+mzs1Size)).node();
                mousemoveGeneral.bind(node)(spectrum2.peaks[i],(i+mzs1Size));
            }, function() {
                mouseleaveGeneral();
            })
        );
    });
    tooltip.on("mousemove", function(){ translateHover(d3.event.clientX, d3.event.clientY); });
};

function spectraViewer(json){
    init();
    data = json;
    if (json.spectra[1] == null) { // 1. mode
        if (json.spectra[0].name.includes("MS1") || (anno_str.length === 0 && svg_str === null)) {
            // 1.1 Mode: MS1, MS2 without structureViewer
            spectrumPlot(json.spectra[0], false);
        } else { // 1.2 Mode: MS2 + StructureViewer
            spectrumPlot(json.spectra[0], true);
        }
    } else { // 2. mode
        mirrorPlot(json.spectra[0], json.spectra[1], view.style, view.intensity);
    }
};

//var debug = d3.select("body")
//    .append("div").html("DEBUG");

function loadJSONData(data_spectra, data_highlight, data_svg) {
    if (data !== undefined) {
        d3.select("#container").html("");
        domain_fix = {xMin: null, xMax: null};
        domain_tmp = {xMin: null, xMax: null, yMax: null};
        pan.mouseupCheck = false;
        pan.mousemoveCheck = false;
        selected = {leftClick: null, hover: null};
        basic_structure = null;
    }
    anno_str = [];
    svg_str = null; // PLEASE get rid of all global variables
    if (data_highlight !== null && data_svg !== null) {
        if ((typeof data_highlight) == "string") {
            anno_str = JSON.parse(data_highlight);
        } else {
            anno_str = data_highlight;
        }
        svg_str = data_svg;
    }
    if ((typeof data_spectra) == "string") {
        spectraViewer(JSON.parse(data_spectra));
    } else {
        spectraViewer(data_spectra);
    }
    return true;
}
window.loadJSONData = loadJSONData;
