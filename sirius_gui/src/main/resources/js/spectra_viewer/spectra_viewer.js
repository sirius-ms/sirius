// General Settings
var svg, tooltip, peakArea, brush, idleTimeout, data, w, h, xmin_tmp, xmax_tmp,
current = {w, h},
margin = {top: 25, right: 30, bottom: 70, left:60},
peakWidth = 2,
decimal_place = 4,
col = {annotation: "lightcoral", spec1: "royalblue",  spec2: "limegreen"},
view = {mirror: "normal"}; // alternativ: "simple"

d3.select("body")
    .append("div")
        .attr("id", "container")
        .style("vertical-align", "top")
        .style("width", "100%")
        .style("position", "relative")
        .style("display", "inline-block")
        .style("margin", 0);

window.addEventListener("resize", function(){
    d3.select("#container").html("");
    spectraViewer(data);
});

window.addEventListener('contextmenu', event => event.preventDefault());

function idled() { idleTimeout = null; };

function firstNChar(str, num) {
    if (str.length > num) {
        return str.slice(0, num);
    } else {
        return str;
    }
};

function translateHover(mouse_w, mouse_h) {
    // NOTE: These distances might need to be changed, when the font size and content of hover are changed.
    // current hover style: padding = 5px, border-width = 1px
    let tmp_tooltip = document.querySelector("#tooltip");
    let tmp_h = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("height")) + 12;
    let tmp_w = parseFloat(window.getComputedStyle(tmp_tooltip).getPropertyValue("width")) + 12;
    if (mouse_h+tmp_h+15 > current.h) {
        tooltip.style("top", (mouse_h - 15 - tmp_h/2 + "px"));
    } else {
        tooltip.style("top", (mouse_h + 15 + "px"));
    }
    if (mouse_w+tmp_w+15 > current.w) {
        tooltip.style("left", (mouse_w - 15 - tmp_w + "px"));
    } else {
        tooltip.style("left", (mouse_w + 15 + "px"));
    }
};

function showHover(d) {
    if ("formula" in d) {
        let sign = "";
        if (d.massDeviationMz > 0) {
            sign = "+";
        }
        tooltip.html("Formula: " + d.formula.replace(" + [M", "").replace("]","") +
                     "<br>Intensity: " + d.intensity.toFixed(decimal_place) +
                     "<br>m/z: " + d.mz.toFixed(decimal_place) +
                     "<br>Mass deviation: " + sign + (d.massDeviationMz*1000).toFixed(decimal_place) + " mDa<br>" +
                     "&nbsp;".repeat(25) + "(" + sign + d.massDeviationPpm.toFixed(decimal_place) + " ppm)");
    } else {
        tooltip.html("m/z: " + d.mz.toFixed(decimal_place) + "<br>Intensity: " + d.intensity.toFixed(decimal_place));
    }
};

function hideHover() {
    tooltip.style("opacity", 0);
    tooltip.html("");
};

var mouseover = function() {
    tooltip.style("opacity", 1);
    d3.select(this).attr("fill", col.annotation);
};

var mousemove = function(d) {
    showHover(d);
    let event = window.event;
    translateHover(event.clientX, event.clientY);
};

function resize() {
    current = {w: window.innerWidth, h: window.innerHeight};
    w = current.w - margin.left - margin.right;
    h = current.h - margin.top - margin.bottom;
};

// For functions outside the js script to clear SVG (e.g. when the input is deleted)
function clear() {
    d3.select("#container").html("");
    data = null;
    xmin_tmp = null;
    xmax_tmp = null;
};

function init() {
    resize();
    svg = d3.select("#container")
        .append('svg')
        .attr("id", "svg-responsive")
        .style("top", 0)
        .style("left", 0)
        .attr("height", current.h)
        .attr("width", current.w)
        .style("position", "absolute")
        .append("g")
            .attr("id", "content")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    // X label
    svg.append("text")
        .attr("class", "label")
        .attr("x", w/2)
        .attr("y", h + margin.top + 10)
        .text("m/z");
    // Y label
    svg.append("text")
        .attr("class", "label")
        .attr("transform", "rotate(-90)")
        .attr("y", -40)
        .attr("x", -h/2)
        .text("Relative intensity");

    svg.selectAll(".label")
        .attr("text-anchor", "middle")
        .attr("opacity", 0);
    //tooltip
    tooltip = d3.select("#container")
        .append("div")
        .attr("id", "tooltip")
        .style("position", "absolute")
        .style("opacity", 0);
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
        .attr("clip-path", "url(#clip)")

    peakArea.append("g")
        .attr("id", "brushArea");
};

function spectrumPlot(spectrum) {
    let mzs = spectrum.peaks.map(d => d.mz);
    let min = d3.min(mzs)-1;
    let max = d3.max(mzs)+1;
    if (xmin_tmp === undefined || xmin_tmp === null) {
        xmin_tmp = min;
        xmax_tmp = max;
    }
    // X axis
    var x = d3.scaleLinear()
        .range([0, w])
        .domain([xmin_tmp, xmax_tmp]);
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
            xmin_tmp = min;
            xmax_tmp = max;
        } else {
            xmin_tmp = x.invert(extent[0]);
            xmax_tmp = x.invert(extent[1]);
            x.domain([ xmin_tmp, xmax_tmp ])
            peakArea.select("#brushArea").call(brush.move, null)
        }
        xAxis.transition().duration(1000).call(d3.axisBottom(x))
        peakArea.selectAll(".peak")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("height", function(d) { return h - y(d.intensity); })
    };
    brush = d3.brushX().extent( [ [0,0], [w, h] ])
    brush.on("end", updateChart);
    peakArea.select("#brushArea").call(brush);
    // add Peaks
    peakArea.selectAll()
        .data(spectrum.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("width", peakWidth)
            .attr("height", function(d) { return h - y(d.intensity); })
            .attr("fill", function(d) {
                if ("formula" in d) {
                    return col.spec2;
                } else {
                    return col.spec1;}})
            .on("mouseover", mouseover)
            .on("mousemove", mousemove)
            .on("mouseleave", function(d) {
                hideHover();
                if ("formula" in d) {
                    d3.select(this).attr("fill", col.spec2);
                } else {
                    d3.select(this).attr("fill", col.spec1);
                }});
};

function mirrorPlot(spectrum1, spectrum2, view) {
    let mzs1 = spectrum1.peaks.map(d => d.mz);
    let mzs2 = spectrum2.peaks.map(d => d.mz);
    let min = d3.min([d3.min(mzs1), d3.min(mzs2)])-1;
    let max = d3.max([d3.max(mzs1), d3.max(mzs2)])+1;
    if (xmin_tmp === undefined || xmin_tmp === null) {
        xmin_tmp = min;
        xmax_tmp = max;
    }
    // X axis
    var x = d3.scaleLinear()
        .range([0, w-20])
        .domain([xmin_tmp, xmax_tmp]);
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
        .attr("class", "legend")
        .attr("x", -h/4)
        .text(firstNChar(spectrum1["name"], 20));

    svg.append("text")
        .attr("class", "legend")
        .attr("x", -h*3/4)
        .text(firstNChar(spectrum2["name"], 20));

    svg.selectAll(".legend")
        .attr("y", w)
        .attr("text-anchor", "middle")
        .attr("transform", "rotate(-90)")
        .style("fill", "gray");

    // brushing
    function updateChart() {
        extent = d3.event.selection
        if(!extent){
            if (!idleTimeout) return idleTimeout = setTimeout(idled, 350);
            x.domain([min, max])
            xmin_tmp = min;
            xmax_tmp = max;
        } else {
            xmin_tmp = x.invert(extent[0]);
            xmax_tmp = x.invert(extent[1]);
            x.domain([ xmin_tmp, xmax_tmp ])
            peakArea.select("#brushArea").call(brush.move, null)
        }
        xAxis.transition().duration(1000).call(d3.axisBottom(x))
        peakArea.selectAll("#peak1")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); })
        peakArea.selectAll("#peak2")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); })
    };
    brush = d3.brushX().extent( [ [0,0], [w-20, h] ])
    brush.on("end", updateChart);
    peakArea.select("#brushArea").call(brush);
    // Peaks 1
    peakArea.selectAll()
        .data(spectrum1.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak")
            .attr("id", "peak1")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); })
            .attr("fill", col.spec1)
            .on("mouseleave", function(d) {
                hideHover();
                d3.select(this).attr("fill", col.spec1);
            });
    // Peaks 2
    peakArea.selectAll()
        .data(spectrum2.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak")
            .attr("id", "peak2")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); })
            .attr("fill", col.spec2)
            .on("mouseleave", function() {
                hideHover();
                d3.select(this).attr("fill", col.spec2); });

    peakArea.selectAll(".peak")
        .attr("width", peakWidth)
        .on("mousemove", mousemove)
        .on("mouseover", mouseover);
};

function spectraViewer(json){
    init();
    if (json.spectra[1] == null) { //null==null und undefined
	    spectrumPlot(json.spectra[0]);
	} else {
	    mirrorPlot(json.spectra[0], json.spectra[1], view.mirror);
	}
	data = json;
};

//var debug = d3.select("body")
//    .append("div").html("DEBUG");

function loadJSONData(data_spectra) {
//    debug.text("got json input");
    if (data !== undefined) {
        d3.select("#container").html("");
        xmin_tmp = null;
        xmax_tmp = null;
    }
    spectraViewer(JSON.parse(data_spectra));
}

// d3.json("bicuculline.json").then(spectraViewer);
