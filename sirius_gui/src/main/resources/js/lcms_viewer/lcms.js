'use strict';

class LiquidChromatographyPlot {
    constructor(svgSelector) {
        this.svgSelector = svgSelector;
        this.svg = d3.select(svgSelector);
        this.width = 800;
        this.height = 400;
        this.margin = { top: 20, right: 30, bottom: 40, left: 100 };
        this.plotWidth = this.width - this.margin.left - this.margin.right;
        this.plotHeight = this.height - this.margin.top - this.margin.bottom;
        this.order = "ALPHABETICALLY";
        this.zoomedIn = false;
        this.unit = "s";
        this.initPlot();
        this.initZoom();
    }

    loadJsonForCompound(json, mainFeature) {
        this.data = new LCCompoundData(json, mainFeature);
        this.clear();
        this.createPlot();
    }

    loadJson(json) {
        this.data = new LCAlignmentData(json);
        this.clear();
        this.createPlot();
    }

    loadString(jsonString) {
        this.data = new LCAlignmentData(JSON.parse(jsonString));
        this.clear();
        this.createPlot();
    }

    loadStringForCompound(jsonString, mainFeature) {
        this.data = new LCCompoundData(JSON.parse(jsonString), mainFeature);
        this.clear();
        this.createPlot();
    }

    setOrder(order) {
        this.order = order;
    }

    setRtUnit(value) {
        if (value=="min") {
            this.unit = "min";
        } else if (value=="s") {
            this.unit = "s";
        }
        this.clear();
        this.initPlot();
        this.createPlot();
    }

    loadData(dataUrl, afterwards, compoundMainFeature=null) {
        d3.json(dataUrl).then(data => {
            this.data = compoundMainFeature===null ? new LCAlignmentData(data) : new LCCompoundData(data, compoundMainFeature);
            this.clear();
            this.createPlot();
            if (afterwards) {
                afterwards.call();
            }
        }).catch(error => {
            console.log(error);
        });
    }

    focusOn(index) {
        this.removeFocus();
        this.focusedItem = d3.select(`#sample-${index}`);
        this.focusedCurve = d3.select(`#curve-${index}`);
        this.focusedItem.classed("lcfocused", true);
        this.focusedCurve.classed("lcfocused", true);
    }

    removeFocus() {
        if (this.focusedItem) {
            this.focusedItem.classed("lcfocused",false);
            this.focusedItem = null;
        }
        if (this.focusedCurve) {
            this.focusedCurve.classed("lcfocused",false);
            this.focusedCurve = null;
        }
    }

    clear() {
        this.svg.selectChildren().remove();
        this.initPlot();
    }

    updateSamples() {
        const intensity = d3.format(" >.4p");
        let comparisonFunction;
        if (this.order == "ALPHABETICALLY") {
            comparisonFunction = (u,v)=>u.label.localeCompare(v.label);
        }
        
        if (this.order == "BY_INTENSITY") {
            comparisonFunction = (u,v)=>v.relativeMainFeatureIntensity-u.relativeMainFeatureIntensity;
        }
        this.samples = d3.select("#lclegend-container");
        this.samples.selectAll("ul").remove();

        this.items = this.samples.append("ul").classed("lclegend-list", true).selectAll("li").data(this.data.traces).join("li")
            .classed("lclegend-item", true).attr("id", (d)=>`sample-${d.index}`).sort(comparisonFunction);

        this.items.append("span").classed("lccolor-box", true).style("background-color", (d)=>d.color);
        this.items.append("span").classed("lcsample-name", true).text((d)=>d.label);
        this.items.append("span").classed("lcintensity-value", true).text((d)=>intensity(d.relativeMainFeatureIntensity));
        this.items.on("click", (e,d)=>d.mainFeature()===null ? null : this.zoomToFeature(d.mainFeature()));

        this.items.on("mouseover", (e,d)=>this.focusOn(d.index))
            .on("mouseout", (e,d)=>(d.mainFeature()===null) ? null : this.removeFocus(d.mainFeature()));

    }

    initPlot() {
        this.svg.append('defs').append('clipPath')
            .attr('id', 'clip')
            .append('rect')
            .attr('width', this.plotWidth)
            .attr('height', this.plotHeight);
        this.plotArea = this.svg.append('g')
            .attr('class', 'plot-area')
            .attr('transform', `translate(${this.margin.left},${this.margin.top})`);

        this.plotArea.append('g')
            .attr('class', 'x-axis')
            .attr('transform', `translate(0,${this.plotHeight})`);

        this.plotArea.append('g')
            .attr('class', 'y-axis');

        this.svg.append("text").classed("legend", true).attr("x", this.plotWidth/2).attr("y",this.height).text("retention time (" + this.unit + ")");
        this.svg.append("text").classed("legend", true).attr("transform", `translate(10, ${this.height/2}) rotate(-90)`).text("intensity");

    }

    initZoom() {
        this.zoomY = d3.zoom()
            .scaleExtent([1, 10])
            .translateExtent([[this.margin.left, this.margin.top], [this.width - this.margin.right, this.height - this.margin.bottom]])
            .extent([[this.margin.left, this.margin.top], [this.width - this.margin.right, this.height - this.margin.bottom]]);
        this.svg.call(this.zoomY);
    }

    zoomedY(event) {
        const transform = event.transform;
        this.yScale.range([this.plotHeight, 0].map(d => transform.applyY(d)));

        this.plotArea.select('.y-axis').call(this.yAxis);
        this.plotArea.select('.lcline').attr('d', this.line);
        this.updatePlot();
    }

    createPlot() {
        this.updateSamples();
        if (this.zoomOut) this.zoomOut.remove();
        this.zoomedIn = false;

        // Define scales and axes
        this.xScale = d3.scaleLinear()
            .domain(this.data.retentionTimeDomain())
            .range([0, this.plotWidth]);
        this.yScale = d3.scaleLinear()
            .domain(this.data.intensityDomain())
            .range([this.plotHeight, 0]);

        this.xAxis = d3.axisBottom(this.xScale);
        this.yAxis = d3.axisLeft(this.yScale);

        this.plotArea.select('.x-axis').call(this.xAxis);
        this.plotArea.select('.y-axis').call(this.yAxis);

        if (this.mainPlot) this.mainPlot.remove();
        this.mainPlot = this.plotArea.append("g").attr('clip-path', 'url(#clip)');

        if (this.featureArea) this.featureArea.remove();
        if (this.data.empty) return;

        // feature area
        this.featureArea = this.mainPlot.append("g").selectAll()
            .data(this.data.focusFeatures).join("rect")
            .attr("id", (d)=>"area"+d.label)
            .attr("class", (d)=>(d.getCSSClasses()))
            .attr("x", (d)=>this.xScale(d.fromRt))
            .attr("y", 0)
            .attr("width", (d)=>this.xScale(d.toRt)-this.xScale(d.fromRt))
            .attr("height", this.plotHeight);
        // Define line generator
        this.line = d3.line()
            .x(d => this.xScale(d.rt))
            .y(d => this.yScale(d.intensity));

        this.noiseLevel = this.mainPlot.append("path").datum(this.data.noiseLevel).attr("class", "noiselevel").attr("d", this.line).attr("id","noise-curve").style("fill","none")


        this.traces=[];
        // add all other traces
        for (var i=0; i < this.data.traces.length; ++i) {
            let tr = this.data.traces[i];
            let j=i;
            let trace = this.mainPlot.append('path')
                    .datum(tr.data())
                    .attr('class', "lccurve lclines")
                    .attr('d', this.line)
                    .attr("id", (d)=>`curve-${j}`)
                    .style('fill', 'none')
                    .style('stroke', tr.color)
                    .on("mouseover", (e,d)=>{this.focusOn(j)})
                    .on("mouseout",(e,d)=>this.removeFocus());
                this.traces.push(trace);
        }

        // Add the line to the plot
        if (this.data.specialTrace) {
            this.mergedTrace = this.mainPlot.append('path')
            .datum(this.data.specialTrace.data())
            .attr('class', "lcmaincurve lclines")
            .attr('d', this.line)
            .style('fill', 'none')
            .style('stroke', getEmphColor())
            .style('stroke-width', '3px');
        }

        // add event listeners
        this.featureArea.on('click', (e,d) => this.zoomToFeature(d));

        this.zoomOut = this.plotArea.append("g").attr("transform", "translate(10,5)")
        .on("mouseover", function() {
            d3.select(this).attr("transform", "translate(10,5) scale(1.3)");
        })
        .on("mouseout", function() {
            d3.select(this).attr("transform", "translate(10,5) scale(1)");
        }).attr("visibility","hidden")
        .on("click", () => {
            this.resetZoom();
        });
        // Draw the circle
        this.zoomOut.append("rect").classed("lczoombox",true).attr("x",0).attr("y",0).attr("width",23).attr("height",23).attr("fill","white").attr("fill-opacity","0.0")

        this.zoomOut.append("circle")
            .attr("cx", 11)
            .attr("cy", 11)
            .attr("r", 8)
            .attr("stroke", getEmphColor())
            .attr("fill", "none")
            .attr("stroke-width", 2);

        // Draw the horizontal line (minus sign)
        this.zoomOut.append("line")
            .attr("x1", 7)
            .attr("y1", 11)
            .attr("x2", 15)
            .attr("y2", 11)
            .attr("stroke", getEmphColor())
            .attr("stroke-width", 2);

        // Draw the handle of the magnifying glass
        this.zoomOut.append("line")
            .attr("x1", 16.6569)
            .attr("y1", 16.6569)
            .attr("x2", 23)
            .attr("y2", 23)
            .attr("stroke", getEmphColor())
            .attr("stroke-width", 2);


    }

    resetZoom() {
        // Create a transition
        this.svg.transition().duration(750).call(
            this.zoomTo(this.xScale, this.yScale)
        );
        this.zoomOut.attr("visibility", "hidden");
        this.zoomedIn = false;
    }

    zoomToFeature(feature) {
        this.zoomedIn = true;
        // Get the feature's data

        // Calculate new domains
        const newXDomain = [feature.fromRt, feature.toRt];
        const newYDomain = this.yScale.domain(); // Keep the same y domain for now

        // Create new scales
        const newXScale = d3.scaleLinear()
            .domain(newXDomain)
            .range([0, this.plotWidth]);

        const newYScale = d3.scaleLinear()
            .domain(newYDomain)
            .range([this.plotHeight, 0]);

        // Create a transition
        this.svg.transition().duration(750).call(
            this.zoomTo(newXScale, newYScale)
        );
        this.zoomOut.attr("visibility", "visible");
    }

    zoomTo(newXScale, newYScale) {
        return (transition) => {
            transition.select('.x-axis')
                .call(this.xAxis.scale(newXScale));

            transition.select('.y-axis')
                .call(this.yAxis.scale(newYScale));

            // Update line path
            transition.selectAll('.lclines')
                .attr('d', d => this.line.x(d => newXScale(d.rt)).y(d => newYScale(d.intensity))(d));

            // Update feature area
            transition.selectAll('.lcfeatureBox')
                .attr("x", (d) => newXScale(d.fromRt))
                .attr("width", (d) => newXScale(d.toRt) - newXScale(d.fromRt));
        };
    }   

}


class AbstractLiquidChromatographyData {

    constructor(json) {
        this.json = json;
        this.empty = json.traces.length<=0;
        this.traces = [];
        this.samples = [];
        this.focusFeatures = [];
        this.calculateNormalizations();
    }

    addTrace(trace, sample) {
        this.traces.push(trace)
    }

    addFocusFeature(feature) {
        this.focusFeatures.push(feature);
    }

    setSpecialTrace(trace) {
        this.specialTrace = trace;
        let dom = this.retentionTimeDomain();
        this.noiseLevel = trace.noiseLevel;
        this.noiseLevel[0].rt = dom[0];
        this.noiseLevel[1].rt = dom[1];
    }

    finishDefinition() {
        if (this.empty) return;
        let maxInt = 0.0;
        let maxIntMain=0.0;
        for (var i=0; i < this.traces.length; ++i) {
            this.traces[i].intensity = this.traces[i].featureIntensity();
            this.traces[i].mainFeatureIntensity = this.traces[i].getMainFeatureIntensity();
            maxInt = Math.max(maxInt, this.traces[i].intensity);
            maxIntMain = Math.max(maxIntMain, this.traces[i].mainFeatureIntensity);
        }
        for (var i=0; i < this.traces.length; ++i) {
            this.traces[i].relativeIntensity = this.traces[i].intensity/maxInt;
            this.traces[i].relativeMainFeatureIntensity = this.traces[i].mainFeatureIntensity/maxIntMain;
        }
    }

    retentionTimes() {
        if (this.empty) return [];
        return this.json.axes.retentionTimeInSeconds;
    }

    retentionTimeDomain() {
        if (this.empty) return [0,1];
        let rt = this.retentionTimes();
        return [rt[0]*0.9, rt[rt.length-1]*1.1];
    }

    maxIntensityWithin(rtDomain) {
        if (this.empty) return 1;
        let start = d3.bisectLeft(this.retentionTimes(), rtDomain[0]);
        let end = d3.bisectRight(this.retentionTimes(), rtDomain[1]);
        var maxInt = 0.0;
        for (var i=0; i < this.json.traces.length; ++i) {
            let t = this.json.traces[i];
            for (var j = start; j < end; ++j) {
                maxInt = Math.max(maxInt, t.data_[j].intensity);
            }
        }
        return maxInt;
    }

    intensityDomain() {
        if (this.empty) return [0,1];
        let minInt = 0.0;
        let maxInt = 0.0;
        for (var i=0; i < this.traces.length; ++i) {
            maxInt = Math.max(maxInt, this.traces[i].maxIntensity());
        }
        return [minInt, maxInt*1.1];
    }

    calculateNormalizations() {
        if (this.json.traces.length==0) {
            this.normFactor = 0.0;
        } else {
            let normFactor = 0; let count=0;
            for (var i=0; i < this.json.traces.length; ++i) {
                let t = this.json.traces[i];
                if (t.merged !== true) {
                    normFactor += t.normalizationFactor;
                    count += 1;
                }
            }
            this.normFactor = normFactor==0 ? 1.0 : normFactor;
        }
    }

}

class LCAlignmentData extends AbstractLiquidChromatographyData {
    constructor(json) {
        super(json);
        for (var i=0; i < this.json.traces.length; ++i) {
            if (this.json.traces[i].merged===true) {
                let tr = new Trace(this, this.json.traces[i], -1, "");
                tr.color = getEmphColor();
                this.setSpecialTrace(tr);
            } else {
                let tr = new Trace(this, this.json.traces[i], this.traces.length,this.json.traces[i].sampleName);
                this.addTrace(tr);
            }
        }
        this.finishDefinition();
        if (this.specialTrace) {
            for (var l = 0; l < this.specialTrace.featureAnnotations.length; ++l) {
                this.addFocusFeature(this.specialTrace.featureAnnotations[l]);
            }
        }
    }
}

class LCCompoundData extends AbstractLiquidChromatographyData {
    constructor(json, mainFeature) {
        super(json);
        this.mainFeatureId = mainFeature;
        for (var i=0; i < this.json.traces.length; ++i) {
            let tr = new Trace(this, this.json.traces[i], this.traces.length, this.json.traces[i].label);
            this.addTrace(tr);
            if (this.json.traces[i].id==mainFeature) {
                tr.color = getEmphColor();
                this.setSpecialTrace(tr);
            }
        }
        this.finishDefinition();
        if (this.specialTrace) {
            for (var l=0; l < this.specialTrace.featureAnnotations.lenth; ++l) {
                this.addFocusFeature(this.specialTrace.featureAnnotations[l]);
            }
        }
    }

    calculateNormalizations() {
        this.normFactor = 1.0;
    }
}

class Trace {

    constructor(traceset, json, index, label) {
        this.json = json;
        this.traceset = traceset;
        this.index = index;
        this.label = label;
        this.color = index >= 0 ? getColor(index) : null;
        let norm = traceset.normFactor;
        let rts = traceset.retentionTimes();
        let len = rts.length;
        let m = this.isMerged();
        this.data_ = [];
        for (var i=0; i < len; ++i) {
            if (i>=json.intensities.length) {
                this.data_.push(new DataPoint(this, rts[i], 0.0, 0.0));
            }else if (m) {
                this.data_.push(new DataPoint(this, rts[i], json.intensities[i]/norm, json.intensities[i]));
            } else {
                this.data_.push(new DataPoint(this, rts[i], json.intensities[i], json.intensities[i]*json.normFactor));
            }
        }
        this.noiseLevel = [
            new DataPoint(this, rts[0], m ? this.json.noiseLevel/norm : this.json.noiseLevel, m ? this.json.noiseLevel : this.json.noiseLevel*json.normFactor ),
            new DataPoint(this, rts[len-1], m ? this.json.noiseLevel/norm : this.json.noiseLevel, m ? this.json.noiseLevel : this.json.noiseLevel*json.normFactor )

        ];
        this.ms2Annotations = [];
        this.featureAnnotations = [];
        this.mainFeatureId = -1;
        if (json.annotations) {
            for (var i=0; i < json.annotations.length; ++i) {
                let a = json.annotations[i];
                if (a.type=="FEATURE") {
                    var d = new DataSelection(
                        a.index, a.from, a.to, rts[a.index], rts[a.from], rts[a.to], a.description
                    );
                    this.featureAnnotations.push(d);
                    if (d.isMainFeature()) {
                        this.mainFeatureId = this.featureAnnotations.length-1;
                    }
                } else if (a.type=="MS2") {

                }
            }
        }
    }

    getMainFeatureIntensity() {
        return this.featureIntensity(); // currently this is the same
    }

    mainFeature() {
        if (this.mainFeatureId>=0) return this.featureAnnotations[this.mainFeatureId];
        else return null;
    }

    featureIntensity() {
        if (this.mainFeatureId < 0) return 0.0;
        let feature = this.mainFeature();
        return this.data_[feature.apex].intensity;
    }

    ms2Annotations() {

    }

    data() {
        return this.data_;
    }

    maxIntensity() {
        return d3.max(d3.map(this.data_, (d)=>d.intensity));
    }
    maxNormalizedIntensity() {
        return d3.max(d3.map(this.data_, (d)=>d.normalizedIntensity));
    }

    isMerged() {
        return this.json.merged===true;
    }

}

class DataPoint {
    constructor(trace, rt, intensity, normalizedIntensity) {
        this.rt = rt;
        this.intensity = intensity;
        this.normalizedIntensity = normalizedIntensity;
        this.trace = trace;
    }
}

class DataSelection {
    constructor(apex, fromindex, toindex,  apexrt, fromrt, tort, label) {
        this.apex = apex;
        this.apexrt = apexrt;
        this.fromIndex = fromindex;
        this.toIndex = toindex;
        this.fromRt = fromrt;
        this.toRt = tort;
        this.processLabel(label);
    }
    processLabel(label) {
        var tokens=label.split(/\[|\]/);
        this.label = tokens.slice(-1)[0];
        this.qualifiers = tokens.slice(0,-1);
    }
    getCSSClasses() {
        var kl = ["lcfeatureBox"];
        if (this.isMainFeature()) {
            kl.push("mainFeatureBox");
        } else {
            kl.push("secondaryFeatureBox");
        }
        if (this.isTerribleQuality()) {
            kl.push("terribleQualityFeatureBox");
        }
        return kl.join(" ");
    }
    isTerribleQuality() {
        return this.qualifiers.includes("LOWEST") || this.qualifiers.includes("NOT_APPLICABLE"); 
    }
    isMainFeature() {
        return this.qualifiers.includes("MAIN");
    }
}

function getEmphColor() {
    if (isDark()) {
        return "#fff";
    } else {
        return "black";
    }
}

function getColor(index) {
    const colorPalette = [
"#a6cee3",
"#1f78b4",
"#b2df8a",
"#33a02c",
"#fb9a99",
"#e31a1c",
"#fdbf6f",
"#ff7f00",
"#cab2d6",
"#6a3d9a",
"#ffff99",
"#b15928",
"#00876c",
"#439981",
"#6aaa96",
"#8cbcac",
"#aecdc2",
"#cfdfd9",
"#f1f1f1",
"#f1d4d4",
"#f0b8b8",
"#ec9c9d",
"#e67f83",
"#de6069",
"#d43d51"
    ];

    const baseColor = colorPalette[index % colorPalette.length];
    const variationFactor = Math.floor(index / colorPalette.length);
    
    let col = d3.color(baseColor);
    if (isDark()) {
        col = col.brighter(variationFactor/10.0 + 0.1);
    } else {
        col = col.darker(variationFactor/10.0);
    }
    return col.formatHex();
}

var COLOR_MODE = "bright";
function setBright() {
    COLOR_MODE = "bright"; 
}
function setDark() {
    COLOR_MODE = "dark"; 
}
function isBright() {
    return COLOR_MODE==="bright";
}
function isDark() {
    return COLOR_MODE==="dark";
}

function drawPlot(svgSelector, dataUrl) {
    const plot = new LiquidChromatographyPlot(svgSelector);
    if (dataUrl) {
        plot.loadData(dataUrl);
    }
    return plot;
}
document.setDark = setDark;
document.setBright = setBright;
document.drawPlot = drawPlot;