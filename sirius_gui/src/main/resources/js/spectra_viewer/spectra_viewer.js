// General Variables
var svg, tooltip, peakArea, brush, idleTimeout, data, w, h,
current = {w, h},
x_tmp = {min: null, max: null},
margin = {top: 20, outerRight: 30, innerRight: 20, bottom: 65, left: 60},
decimal_place = 4,
// MS2 + structure
strucArea, annoArea, ms2Size,
selected = {leftClick: null, hover: null},
svg_str = null, basic_structure = null,
anno_str = [], highlights = [],
// Mirror Plot
view = {mirror: "normal"}; // alternativ: "simple"


d3.select("body").append("div").attr("id", "container");

window.addEventListener('contextmenu', event => event.preventDefault());

window.addEventListener("resize", function(){
    d3.select("#container").html("");
    spectraViewer(data);
    if (selected.leftClick !== null) {
        showStructure(selected.leftClick);
    }
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
            selected.leftClick = new_selected;
            svg.select("#peak"+selected.leftClick).attr("class", "peak_select peak");
            document.getElementById("anno_leftClick").innerText = annotation(data.spectra[0].peaks[selected.leftClick]).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
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
    selected = {leftClick: null, hover: null};
    svg_str = null;
    basic_structure = null;
    anno_str = [];
    highlights = [];
};

function firstNChar(str, num) { return (str.length > num) ? str.slice(0, num) : str; };

function showStructure(i) {
    if (i === ms2Size - 1) {
        strucArea.html("");
        document.getElementById('str_border').innerHTML = svg_str;
        basic_structure = strucArea.select('svg')
            .style('height', '100%')
            .style('width', '100%');
        strucArea.selectAll(".bond").classed("highlight_bond", true);
        strucArea.selectAll(".atom").classed("highlight_atom", true);
        basic_structure.style("opacity", 1);
    } else if (highlights[i] !== null) {
        strucArea.html("");
        document.getElementById('str_border').innerHTML = svg_str;
        basic_structure = strucArea.select('svg')
            .style('height', '100%')
            .style('width', '100%');
        highlighting(highlights[i].bonds, highlights[i].cuts, highlights[i].atoms);
        basic_structure.style("opacity", 1);
    } else {
        if (basic_structure !== null) basic_structure.style("opacity", 0);
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
        d3.select("#mol1bnd"+(bonds[i]+1)).attr("class", "bond highlight_bond");
    }
    for (let j in cuts) {
        d3.select("#mol1bnd"+(cuts[j]+1)).attr("class", "bond highlight_cut");
    }
    for (let l of rest) {
        d3.select("#mol1bnd"+(l+1)).attr("class", "bond rest_bond");
    }
    d3.selectAll(".atom").attr("class", "atom rest_atom");
    for (let k in atoms) {
        d3.select("#mol1atm"+(atoms[k]+1)).attr("class", "atom highlight_atom");
    }
};

function idled() { idleTimeout = null; };

function resetColor(d) { return ("formula" in d) ? "peak_2 peak" : "peak_1 peak"; };

function translateHover(mouse_w, mouse_h) {
    // NOTE: These distances might need to be changed, when the font size and content of hover are changed.
    // current hover style: padding = 5px, border-width = 1px
    let tmp_tooltip = document.querySelector("#tooltip");
    let tmp_h = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("height")) + 12;
    let tmp_w = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("width")) + 12;
    // style: vertical distance between cursor and hover = 24px (depending on size of cursor)
    if (mouse_h+tmp_h+25 > current.h) {
        tooltip.style("top", (mouse_h - 25 - tmp_h/2 + "px"));
    } else {
        tooltip.style("top", (mouse_h - 25 + "px"));
    }
    // style: horizontal distance between cursor and hover = 12px (depending on size of cursor)
    if (mouse_w+tmp_w+15 > current.w) {
        tooltip.style("left", (mouse_w - 15 - tmp_w + "px"));
    } else {
        tooltip.style("left", (mouse_w + 15 + "px"));
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
    } else {
        anno = "m/z: " + d.mz.toFixed(decimal_place) + "<br>Intensity: " + d.intensity.toFixed(decimal_place);
    }
    return anno;
};

function sortByKey(array, key){
    return array.sort(function(a, b) {
        let x = a[key];
        let y = b[key];
        return ((x < y) ? -1
             : ((x > y) ? 1 : 0));
    });
};

function loadHighlights(mzs) {
    if (highlights.length === 0) {
        for (var i = 0; i<ms2Size; i++) {
            highlights.push(null);
        }
    }
    let sorted_anno = sortByKey(anno_str, 'peakmass');
    var k = 0;
    for (let i in mzs) {
        for (var j = k; j<sorted_anno.length; j++) {
            if (mzs[i] === sorted_anno[k].peakmass) {
                highlights[i] = sorted_anno[k];
                k = j+1;
                break;
            } else if (mzs[i] > sorted_anno[k].peakmass) {
                k = j;
            }
        }
    }
    anno_str = [];
};

var mouseoverMS1 = function() {
    d3.select(this).attr("class", "peak_hover peak");
    tooltip.style("opacity", 1);
};

var mousemoveMS1 = function(d, i) {
    let event = window.event;
    tooltip.html(annotation(d));
    translateHover(event.clientX, event.clientY);
    if (selected.hover !== i) {
        d3.select("#peak"+selected.hover).attr("class", resetColor);
        selected.hover = i;
    }
};

var mousemoveMS2 = function(d, i) {
    let tmp = d3.select(this);
    if (!tmp.classed("peak_select")) {
        let event = window.event;
        tooltip.html(annotation(d));
        translateHover(event.clientX, event.clientY);
        if (selected.hover !== i) {
            d3.select("#peak"+selected.hover).attr("class", resetColor);
            selected.hover = i;
        }
    }
};

var mousedown = function(d, i) {
    if (d3.event.button === 0) {
        let tmp = d3.select(this);
        if (selected.leftClick !== null && tmp.classed("peak_select")) { // cancel the selection
            selected.leftClick = null;
            tmp.attr("class", resetColor);
            document.getElementById("anno_leftClick").innerText = "Left click to choose a peak...";
            annoArea.attr("id", "nothing");
            if (basic_structure !== null) basic_structure.style("opacity", 0);
        } else { // create a new selection
            if (selected.leftClick !== null && !tmp.classed("peak_select")) { //cancel the last selection
                d3.select("#peak"+selected.leftClick).attr("class", resetColor);
            }
            selected.leftClick = i;
            tmp.attr("class", "peak_select peak");
            annoArea.attr("id", "anno_leftClick");
            document.getElementById("anno_leftClick").innerText = annotation(d).replace(/<br>/g, "\n").replace(/&nbsp;/g, "");
            showStructure(i);
            selected.hover = null;
            hideHover();
        }
    }
};

function init() {
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
        .attr("y", h + margin.top + 10)
        .text("m/z");
    // Y label
    svg.append("text")
        .attr("class", "label spectrum_label")
        .attr("transform", "rotate(-90)")
        .attr("y", -40)
        .attr("x", -h/2)
        .text("Relative intensity");

    svg.selectAll(".label")
        .attr("opacity", 0);
    //tooltip
    tooltip = d3.select("#container")
        .append("div")
        .attr("id", "tooltip")
        .style("opacity", 0)
    //clipPath & brushing
    svg.append("defs").append("svg:clipPath")
        .attr("id", "clip")
        .append("svg:rect")
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
            .style("width", w/4+"px")
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
};

function spectrumPlot(spectrum, structureView) {
    let mzs = spectrum.peaks.map(d => d.mz);
    ms2Size = mzs.length;
    if (structureView) {
        initStructureView();
        loadHighlights(mzs);
    }
    let min = d3.min(mzs)-1;
    let max = d3.max(mzs)+1;
    if (x_tmp.min === undefined || x_tmp.min === null) {
        x_tmp.min = min;
        x_tmp.max = max;
    }
    // X axis
    var x = d3.scaleLinear()
        .range([0, w])
        .domain([x_tmp.min, x_tmp.max]);
    var xAxis = svg.append("g")
        .attr("transform", "translate(0," + h + ")")
        .call(d3.axisBottom(x));
    // Y axis
    var y = d3.scaleLinear()
        .domain([0, 1])
        .range([h, 0]);
    svg.append("g").call(d3.axisLeft(y));
    svg.selectAll(".label").attr("opacity", 1);
    // brushing
    function updateChart() {
        extent = d3.event.selection
        if(!extent){
            if (!idleTimeout) return idleTimeout = setTimeout(idled, 350);
            x.domain([min, max])
            x_tmp.min = min;
            x_tmp.max = max;
        } else {
            x_tmp.min = x.invert(extent[0]);
            x_tmp.max = x.invert(extent[1]);
            x.domain([ x_tmp.min, x_tmp.max ])
            peakArea.select("#brushArea").call(brush.move, null);
        }
        xAxis.transition().duration(1000).call(d3.axisBottom(x));
        peakArea.selectAll(".peak")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("height", function(d) { return h - y(d.intensity); });
    };
    brush = d3.brushX().extent( [ [0,0], [w, h] ]);
    brush.on("end", updateChart);
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
        peakArea.selectAll(".peak")
            .on("mousedown", mousedown)
            .on("mousemove", mousemoveMS2)
            .on("mouseover", function(d) {
                d3.select(this).call(function(selection) {
                    if (!selection.classed("peak_select")) {
                        selection.attr("class", "peak_hover peak");
                        tooltip.style("opacity", 1);
                    }
                })
            })
            .on("mouseleave", function(d) {
                if (selected.hover !== null) {
                    d3.select("#peak"+selected.hover).attr("class", resetColor);
                    selected.hover = null;
                    hideHover();
                }
            });
    } else {
        peakArea.selectAll(".peak")
            .on("mousemove", mousemoveMS1)
            .on("mouseover", mouseoverMS1)
            .on("mouseleave", function(d) {
                hideHover();
                d3.select(this).attr("class", resetColor);
            });
    }
};

function mirrorPlot(spectrum1, spectrum2, view) {
    let mzs1 = spectrum1.peaks.map(d => d.mz);
    let mzs2 = spectrum2.peaks.map(d => d.mz);
    let min = d3.min([d3.min(mzs1), d3.min(mzs2)])-1;
    let max = d3.max([d3.max(mzs1), d3.max(mzs2)])+1;
    if (x_tmp.min === undefined || x_tmp.min === null) {
        x_tmp.min = min;
        x_tmp.max = max;
    }
    // X axis
    var x = d3.scaleLinear()
        .range([0, w-20])
        .domain([x_tmp.min, x_tmp.max]);
    var xAxis;
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
    svg.selectAll(".label").attr("opacity", 1);
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
    // brushing
    function updateChart() {
        extent = d3.event.selection
        if(!extent){
            if (!idleTimeout) return idleTimeout = setTimeout(idled, 350);
            x.domain([min, max])
            x_tmp.min = min;
            x_tmp.max = max;
        } else {
            x_tmp.min = x.invert(extent[0]);
            x_tmp.max = x.invert(extent[1]);
            x.domain([ x_tmp.min, x_tmp.max ])
            peakArea.select("#brushArea").call(brush.move, null);
        }
        xAxis.transition().duration(1000).call(d3.axisBottom(x));
        peakArea.selectAll(".peak_1")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); });
        peakArea.selectAll(".peak_2")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); });
    };
    brush = d3.brushX().extent( [ [0,0], [w-20, h] ]);
    brush.on("end", updateChart);
    peakArea.select("#brushArea").call(brush);
    // Peaks 1
    peakArea.selectAll()
        .data(spectrum1.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak_1 peak")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); })
            .on("mouseleave", function() {
                hideHover();
                d3.select(this).attr("class", "peak_1 peak");
            });
    // Peaks 2
    peakArea.selectAll()
        .data(spectrum2.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak_2 peak")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); })
            .on("mouseleave", function() {
                hideHover();
                d3.select(this).attr("class", "peak_2 peak");
            });
    peakArea.selectAll(".peak")
        .on("mousemove", mousemoveMS1)
        .on("mouseover", mouseoverMS1);
};

function spectraViewer(json){
    init();
    if (json.spectra[1] == null) { // 1. mode
        if (json.spectra[0].name.includes("MS1") || ((anno_str.length === 0 || highlights.length === 0 ) && svg_str === null)) {
            // 1.1 Mode: MS1, MS2 without structureViewer
            spectrumPlot(json.spectra[0], false);
        } else { // 1.2 Mode: MS2 + StructureViewer
            spectrumPlot(json.spectra[0], true);
        }
    } else { // 2. mode
        mirrorPlot(json.spectra[0], json.spectra[1], view.mirror);
    }
    data = json;
};

//var debug = d3.select("body")
//    .append("div").html("DEBUG");

function loadJSONData(data_spectra, data_highlight, data_svg) {
    if (data !== undefined) {
        d3.select("#container").html("");
        x_tmp = {min: null, max: null};
        selected = {leftClick: null, hover: null};
        basic_structure = null;
        highlights = [];
    }
    if (data_highlight !== null && data_svg !== null) {
        anno_str = JSON.parse(data_highlight);
        svg_str = data_svg;
    }
    spectraViewer(JSON.parse(data_spectra));

}

// d3.json("bicuculline.json").then(spectraViewer);
