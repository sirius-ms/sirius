// General Settings
var svg, brush, idleTimeout, data, w, h,
current = {w, h},
margin = {top: 30, right: 30, bottom: 50, left:60}, // TODO
peakWidth = 2,
font = {family: "sans-serif", size: {hover: "12px", label: "13px", legend: "13px"}},
col = {annotation: "lightcoral", spec1: "royalblue",  spec2: "mediumseagreen", hoverbg: "white"},
view = {mirror: "normal"}; // alternativ: "simple"

d3.select("body")
    .append("div")
        .attr("id", "container")
        .style("vertical-align", "top")
        .style("width", "100%")
        .style("position", "relative")
        .style("display", "inline-block")
        .style("margin", 0)

window.addEventListener("resize", reset);

var mouseover = function() {
    d3.select("#tooltip").style("opacity", 1);
    d3.select(this).attr("fill", col.annotation);
};

var mousemove1 = function(d) {
    d3.select("#tooltip").html("m/z: " + d.mz + "<br>Intensity: " + d.intensity)
        .style("left", (d3.mouse(this)[0]+70 + "px"))
        .style("top", (d3.mouse(this)[1]+60 + "px"));
};

var mouseleave1 = function() {
    d3.select("#tooltip").style("opacity", 0);
    d3.select(this).attr("fill", col.spec1);
};

// TODO: resize
function firstNChar(str, num) {
    if (str.length > num) {
        return str.slice(0, num);
    } else {
        return str;
    }
};

function idled() { idleTimeout = null; };

function resize() {
    current = {w: window.innerWidth, h: window.innerHeight};
    w = current.w - margin.left - margin.right;
    h = current.h - margin.top - margin.bottom;
};

function reset() { // TODO: if data is changed...
    d3.select("#container").remove();
    spectraViewer(data);
};

function init() {
    resize();
    d3.select("body")
        .append("div")
            .attr("id", "container")
            .style("vertical-align", "top")
            .style("width", "100%")
            .style("position", "relative")
            .style("display", "inline-block")
            .style("margin", 0);

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
        .attr("y", h + margin.top + 5)
        .text("m/z")
        .attr("opacity", 0);
    // Y label
    svg.append("text")
        .attr("class", "label")
        .attr("transform", "rotate(-90)")
        .attr("y", -40)
        .attr("x", -h/2)
        .text("Relative intensity")
        .attr("opacity", 0);

    svg.selectAll(".label")
        .attr("font-family", font.family)
        .attr("text-anchor", "middle")
        .attr("font-size", font.size.label);
    //tooltip
    d3.select("#container")
        .append("div")
        .attr("id", "tooltip")
        .style("font-family", font.family)
        .style("font-size", font.size.hover)
        .style("position", "absolute")
        .style("opacity", 0)
        .style("background-color", col.hoverbg)
        .style("border", "solid")
        .style("border-width", "1px")
        .style("border-radius", "5px")
        .style("padding", "5px");
    //clipPath & brushing
    svg.append("defs").append("svg:clipPath")
        .attr("id", "clip")
        .append("svg:rect")
        .attr("width", w )
        .attr("height", h )
        .attr("x", 0)
        .attr("y", 0);

    svg.append("g")
        .attr("id", "peaks")
        .attr("clip-path", "url(#clip)")
        .append("g")
            .attr("id", "brushArea");

    brush = d3.brushX().extent( [ [0,0], [w, h] ])
};

function spectrumPlot(spectrum) {
    let mzs = spectrum.peaks.map(d => d.mz);
    let min = d3.min(mzs)-0.5;
    let max = d3.max(mzs)+0.5;
    // X axis
    var x = d3.scaleLinear()
        .range([0, w])
        .domain([min, max]);
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
        } else {
            x.domain([ x.invert(extent[0]), x.invert(extent[1]) ])
            svg.select("#brushArea").call(brush.move, null)
        }
        xAxis.transition().duration(1000).call(d3.axisBottom(x))
        svg.selectAll(".peak")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("height", function(d) { return h - y(d.intensity); })
    }

    brush.on("end", updateChart);
    svg.select("#brushArea").call(brush);
    // add Peaks
    svg.selectAll()
        .data(spectrum.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y(d.intensity); })
            .attr("width", peakWidth)
            .attr("height", function(d) { return h - y(d.intensity); })
            .attr("fill", col.spec1)
            .on("mouseover", mouseover)
            .on("mousemove", mousemove1)
            .on("mouseleave", mouseleave1);
};

function mirrorPlot(spectrum1, spectrum2, view) {
    let mzs1 = spectrum1.peaks.map(d => d.mz);
    let mzs2 = spectrum2.peaks.map(d => d.mz);
    let min = d3.min([d3.min(mzs1), d3.min(mzs2)])-0.5;
    let max = d3.max([d3.max(mzs1), d3.max(mzs2)])+0.5;
    // X axis
    var x = d3.scaleLinear()
        .range([0, w])
        .domain([min, max]);
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
        .attr("font-family", font.family)
        .attr("font-size", font.size.legend)
        .style("fill", "gray");

    // brushing
    function updateChart() {
        extent = d3.event.selection
        if(!extent){
            if (!idleTimeout) return idleTimeout = setTimeout(idled, 350);
            x.domain([min, max])
        } else {
            x.domain([ x.invert(extent[0]), x.invert(extent[1]) ])
            svg.select("#brushArea").call(brush.move, null)
        }
        xAxis.transition().duration(1000).call(d3.axisBottom(x))
        svg.selectAll("#peak1")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); })
        svg.selectAll("#peak2")
            .transition().duration(1000)
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); })
    }

    brush.on("end", updateChart);
    svg.select("#brushArea").call(brush);
    // Peaks 1
    svg.selectAll()
        .data(spectrum1.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak")
            .attr("id", "peak1")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", function(d) { return y1(d.intensity); })
            .attr("height", function(d) { return h/2 - y1(d.intensity); })
            .attr("fill", col.spec1)
            .on("mousemove", mousemove1)
            .on("mouseleave", mouseleave1);
    // Peaks 2
    svg.selectAll()
        .data(spectrum2.peaks)
        .enter()
        .append("rect")
            .attr("class", "peak")
            .attr("id", "peak2")
            .attr("x", function(d) { return x(d.mz); })
            .attr("y", h/2)
            .attr("height", function(d) { return y2(d.intensity); })
            .attr("fill", col.spec2)
            .on("mousemove", function(d) {
                d3.select("#tooltip").html("m/z: " + d.mz + "<br>Intensity: " + d.intensity)
                .style("left", (d3.mouse(this)[0]+70 + "px"))
                .style("top", (d3.mouse(this)[1]+60 + "px")); })
            .on("mouseleave", function() {
                d3.select("#tooltip").style("opacity", 0);
                d3.select(this).attr("fill", col.spec2); });

    svg.selectAll(".peak")
        .attr("width", peakWidth)
        .on("mouseover", mouseover);
};

function spectraViewer(json){
    data = json;
    init();
    if (json.spectrum2 == null) { //null==null und undefined
		// 1. mode
		spectrumPlot(json.spectrum1);
	} else {
		// 2. mode
		mirrorPlot(json.spectrum1, json.spectrum2, view.mirror);
	}
};

var debug = d3.select("body")
    .append("div").html("DEBUG");

function loadJSONSpectra(input) {
    debug.text("got json input");
    spectraViewer(JSON.parse(input));
}

// d3.json("bicuculline.json").then(spectraViewer);
