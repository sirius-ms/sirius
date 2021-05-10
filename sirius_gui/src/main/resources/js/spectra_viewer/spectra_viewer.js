'use strict';
const TOLERANCE = 40;
// General Variables
var svg, tooltip, peakArea, brush, zoom, idleTimeout, data, w, h, x, xAxis,
current = {w, h},
x_tmp = {min: null, max: null},
x_fix = {min: null, max: null},
scale_tmp = {X: null, Y: null}, //Y for future
pan = {mousedownCheck: false, mousemoveCheck: false, tolerance: 10, step: 500},
margin = {top: 20, outerRight: 30, innerRight: 20, bottom: 65, left: 60},
decimal_place = 4,
// MS2 + structure
strucArea, annoArea, ms2Size,
selected = {leftClick: null, hover: null},
svg_str = null, basic_structure = null,
anno_str = [],
// Mirror Plot
view = {mirror: "normal"}; // alternativ: "simple"


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

document.onkeyup = function(e) {
    if (selected.leftClick !== null) {
        var new_selected = -1;
        if (e.keyCode === 37 && selected.leftClick !== 0) { // left
            new_selected = selected.leftClick - 1;
        } else if (e.keyCode === 39 && selected.leftClick !== ms2Size-1) { // right
            new_selected = selected.leftClick + 1;
        }
        if (new_selected !== -1) {
            svg.select("#peak"+selected.leftClick).attr("class", resetColor);
            if (selected.leftClick === selected.hover) {
                svg.select("#peak"+selected.leftClick).classed("peak_hover", true);
            }
            selected.leftClick = new_selected;
            svg.select("#peak"+selected.leftClick).classed("peak_select", true);
            const selectedPeak = data.spectra[0].peaks[selected.leftClick];
            if (selectedPeak.mz < x_tmp.min || selectedPeak.mz > x_tmp.max && (x_tmp.min !== x_fix.min || x_tmp.max !== x_fix.max)) {
                if (e.keyCode === 37) {
                    setXdomain(selectedPeak.mz-3, x_tmp.max-3);
                } else {
                    setXdomain(x_tmp.min+3, selectedPeak.mz+3);
                }
            }
            document.getElementById("anno_leftClick").innerText = annotation(selectedPeak).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
            showStructure(selected.leftClick);
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
    x_tmp = {min: null, max: null};
    scale_tmp = {X: null, Y: null};
    pan.mousedownCheck = false;
    pan.mousemoveCheck = false;
    selected = {leftClick: null, hover: null};
    svg_str = null;
    basic_structure = null;
    anno_str = [];
};

function showStructure(i) {
    strucArea.html("");
    document.getElementById('str_border').innerHTML = svg_str;
    basic_structure = strucArea.select('svg')
        .style('height', '100%')
        .style('width', '100%');
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
    } else {
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

function idled() { idleTimeout = null; };

// Only used in spectrumPlot: peak_1 = unannotated, peak_2 = annotate
function resetColor(d) {
    let precursor = data.spectra[0].peaks[ms2Size-1].formula;
    if (data.spectra[0].name.includes("MS1")) {
        return (d.peakMatches !== {}) ? "peak_matched peak" : "peak_1 peak";
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
        let tmp = d3.select("#peak"+i);
        if (selected.leftClick !== null && tmp.classed("peak_select")) { // cancel the selection
            selected.leftClick = null;
            tmp.classed("peak_select", false);
            document.getElementById("anno_leftClick").innerText = "Left click to choose a peak...";
            annoArea.attr("id", "nothing");
            showStructure(-1);
        } else { // create a new selection
            if (selected.leftClick !== null && !tmp.classed("peak_select")) { //cancel the last selection
                d3.select("#peak"+selected.leftClick).attr("class", resetColor);
            }
            selected.leftClick = i;
            tmp.classed("peak_select", true);
            annoArea.attr("id", "anno_leftClick");
            document.getElementById("anno_leftClick").innerText = annotation(d).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
            showStructure(i);
            hideHover();
        }
    }
    pan.mousedownCheck = false;
    pan.mousemoveCheck = false;
};

// globally used by spectrumPlot and mirrorPlot (temporarily)
function zoomedX() {
    const transform = d3.event.transform;
    scale_tmp.X = transform.rescaleX(x);
    const newDomain = d3.axisBottom(scale_tmp.X).scale().domain();
    x_tmp.min = (newDomain[0] < 0) ? 0 : newDomain[0];
    x_tmp.max = (newDomain[1] > x_fix.max) ? x_fix.max : newDomain[1];
    x.domain([x_tmp.min, x_tmp.max])
    xAxis.transition().duration(100).call(d3.axisBottom(x));
    peakArea.selectAll(".peak").transition().duration(100).attr("x", function(d) { return x(d.mz); });
    peakArea.select("#brushArea").node().__zoom = d3.zoomIdentity;
};

function setXdomain(newXmin, newXmax) {
    x.domain([newXmin, newXmax])
    x_tmp.min = newXmin;
    x_tmp.max = newXmax;
    scale_tmp.X = x;
    xAxis.transition().duration(50).call(d3.axisBottom(scale_tmp.X));
    peakArea.selectAll(".peak").transition().duration(50).attr("x", function(d) { return scale_tmp.X(d.mz); });
};
// globally used by spectrumPlot and mirrorPlot (temporarily)
function panX() {
    var div = d3.select(this);
    var w = d3.select(window)
        .on("mousedown", mousedownPan)
        .on("mousemove", mousemovePan)
        .on("mouseup", mouseupPan);
    d3.event.preventDefault(); // disable text dragging
    var x0, x1, d, newXmin, newXmax;
    function mousedownPan() {
        if (div.node().id === 'brushArea') {
            pan.mousedownCheck = true;
            x0 = d3.event.clientX;
        }
    }
    function mousemovePan() {
        if (pan.mousedownCheck) {
            x1 = d3.event.clientX;
            d = x1 - x0;
            if (Math.abs(d)>=pan.tolerance) {
                pan.mousemoveCheck = true;
                newXmin = x_tmp.min-d*(x_tmp.max-x_tmp.min)/pan.step;
                newXmax = x_tmp.max-d*(x_tmp.max-x_tmp.min)/pan.step;
                if (newXmin >= 0 && newXmax <= x_fix.max) {
                    setXdomain(newXmin, newXmax);
                }
                x0 = x1;
                d = 0;
            }
        }
    }
    function mouseupPan() {
        if (pan.mousedownCheck && pan.mousemoveCheck) w.on("mousedown", null).on("mousemove", null).on("mouseup", null);
    }
};

function rightClickOnly() { return d3.event.button === 2; };
// globally used by spectrumPlot and mirrorPlot (temporarily)
function brushendX() {
    let extent = d3.event.selection;
    if(!extent){
        if (!idleTimeout) return idleTimeout = setTimeout(idled, 350);
        x.domain([x_fix.min, x_fix.max])
        x_tmp.min = x_fix.min;
        x_tmp.max = x_fix.max;
    } else {
        x_tmp.min = scale_tmp.X.invert(extent[0]);
        x_tmp.max = scale_tmp.X.invert(extent[1]);
        x.domain([x_tmp.min, x_tmp.max])
        peakArea.select("#brushArea").call(brush.move, null);
    }
    xAxis.transition().duration(750).call(d3.axisBottom(x));
    peakArea.selectAll(".peak").transition().duration(750).attr("x", function(d) { return x(d.mz); });
    scale_tmp.X = x;
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
        .attr("transform", "rotate(-90)")
        .attr("y", -40)
        .attr("x", -h/2)
        .text("Relative intensity");

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
                return "Left click to choose a peak...";
            }});
    current.w = current.w/4*3 - 15;
    w = current.w - margin.left - margin.innerRight;
    d3.select("#spectrumView").attr("width", current.w+"px");
    d3.select("#xLabel").attr("x", w/2);
    svg.select("#clipArea").attr("width", w);
};

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

function spectrumPlot(spectrum, structureView) {
    let mzs = spectrum.peaks.map(d => d.mz);
    ms2Size = mzs.length;
    if (structureView) {
        initStructureView();
        injectStructureInformation(spectrum);
    }
    x_fix.min = d3.min(mzs)-3;
    x_fix.max = d3.max(mzs)+3;
    if (x_tmp.min === undefined || x_tmp.min === null) {
        x_tmp.min = x_fix.min;
        x_tmp.max = x_fix.max;
    }
    // X axis
    x = d3.scaleLinear()
        .range([0, w])
        .domain([x_tmp.min, x_tmp.max]);
    scale_tmp.X = x;
    xAxis = svg.append("g")
        .attr("transform", "translate(0," + h + ")")
        .call(d3.axisBottom(x));
    // Y axis
    var y = d3.scaleLinear()
        .domain([0, 1])
        .range([h, 0]);
    svg.append("g").call(d3.axisLeft(y));
    svg.selectAll(".label").attr("visibility", "visible");
    // zoom and pan
    zoom = d3.zoom().extent([[0,0],[w,h]]).on("zoom", zoomedX)
    peakArea.select("#brushArea").call(zoom)
        .on("dblclick.zoom", null)
        .on("mousedown.zoom", panX);
    // brush
    brush = d3.brushX().extent( [ [0,0], [w,h] ]).filter(rightClickOnly).on("end", brushendX);
    peakArea.select("#brushArea").call(brush);
    // add Peaks
    peakArea.selectAll()
        .data(spectrum.peaks)
        .enter()
        .append("rect")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("height", function(d) { return h - y(d.intensity); })
            .attr("id", function(d, i) { return "peak"+i; })
            .attr("class", function(d, i) { return (selected.leftClick === i) ? "peak_select peak" : resetColor(d); });
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

function mirrorPlot(spectrum1, spectrum2, view) {
    let mzs1 = spectrum1.peaks.map(d => d.mz);
    let mzs2 = spectrum2.peaks.map(d => d.mz);
    x_fix.min = d3.min([d3.min(mzs1), d3.min(mzs2)])-1;
    x_fix.max = d3.max([d3.max(mzs1), d3.max(mzs2)])+1;
    if (x_tmp.min === undefined || x_tmp.min === null) {
        x_tmp.min = x_fix.min;
        x_tmp.max = x_fix.max;
    }
    // X axis
    x = d3.scaleLinear()
        .range([0, w-20])
        .domain([x_tmp.min, x_tmp.max]);
    if (view === "normal") {
        xAxis = svg.append("g")
            .attr("transform", "translate(0," + h + ")")
            .call(d3.axisBottom(x));
        svg.append("g")
            .attr("transform", "translate(0," + h/2 + ")")
            .call(d3.axisBottom(x).tickValues([]));
    } else if (view === "simple") {
        xAxis = svg.select("#peaks").append("g")
            .attr("transform", "translate(0," + h/2 + ")")
            .call(d3.axisBottom(x));
    }
    // Y axis 1
    var y1 = d3.scaleLinear()
        .domain([0, 1])
        .range([h/2, 0]);
    svg.append("g").call(d3.axisLeft(y1));
    // Y axis 2
    var y2 = d3.scaleLinear()
        .domain([1, 0])
        .range([h/2, 0]);
    svg.append("g")
        .attr("transform", "translate(0," + h/2 + ")")
        .call(d3.axisLeft(y2));
    svg.selectAll(".label").attr("visibility", "visible");
    // legends: 2 spectrum names
    function firstNChar(str, num) { return (str.length > num) ? str.slice(0, num) : str; };
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
    // zoom and pan
    zoom = d3.zoom().extent([[0,0],[w,h]]).on("zoom", zoomedX)
    peakArea.select("#brushArea").call(zoom)
        .on("dblclick.zoom", null)
        .on("mousedown.zoom", panX);
    // brush
    brush = d3.brushX().extent( [ [0,0], [w-20, h] ]).filter(rightClickOnly).on("end", brushendX);
    peakArea.select("#brushArea").call(brush);
    // Peaks 1
    peakArea.selectAll()
        .data(spectrum1.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak_1 peak")
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
            .attr("id", function(d, i) { return "peak"+(i+mzs1.length); })
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); });

    function upOrDown(currentY, callbackUp, callbackDown) { (currentY <= margin.top+h/2) ? callbackUp() : callbackDown(); };

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
                const node = d3.select("#peak"+(i+mzs1.length)).node();
                mousemoveGeneral.bind(node)(spectrum2.peaks[i],(i+mzs1.length));
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
        mirrorPlot(json.spectra[0], json.spectra[1], view.mirror);
    }
};

//var debug = d3.select("body")
//    .append("div").html("DEBUG");

function loadJSONData(data_spectra, data_highlight, data_svg) {
    if (data !== undefined) {
        d3.select("#container").html("");
        x_tmp = {min: null, max: null};
        scale_tmp = {X: null, Y: null};
        pan.mousedownCheck = false;
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
