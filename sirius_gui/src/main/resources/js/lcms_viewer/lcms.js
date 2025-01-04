'use strict';

class ViewOptions {

    constructor(idPrefix, showLegend=true, showAdductGraph=true, showPlot=true) {
        this.showLegend = showLegend;
        this.showAdductGraph = showAdductGraph;
        this.compoundView = showPlot;
        this.plotId = "#" + idPrefix + "-plot";
        this.legendId = "#" + idPrefix + "-legend";
        this.adductGraphId = "#" + idPrefix + "-graph";
    }

}

class LiquidChromatographyPlot {
    constructor(options) {
        this.options = options;
        this.svgSelector = options.plotId;
        this.svg = d3.select(this.svgSelector);
        this.width = 800;
        this.height = 450;
        this.margin = { top: 20, right: 30, bottom: 40, left: 100 };
        this.plotWidth = this.width - this.margin.left - this.margin.right;
        this.plotHeight = this.height - this.margin.top - this.margin.bottom;
        this.order = "ALPHABETICALLY";
        this.zoomedIn = false;
        this.unit = "s";
        this.initPlot();
        this.initZoom();
    }

    setOptionsForAdductView(){
        this.options.showLegend=false;
        this.options.showAdductGraph=true;
        this.options.compoundView=true;
    }
    setOptionsForAlignmentView() {
        this.options.showLegend=true;
        this.options.showAdductGraph=false;
        this.options.compoundView=true;
    }

    loadJsonForCompound(json, mainFeature) {
        this.setOptionsForAdductView();
        this.data = new LCCompoundData(json, mainFeature);
        this.clear();
        this.createPlot();
    }

    loadJson(json) {
        this.setOptionsForAlignmentView();
        this.data = new LCAlignmentData(json);
        this.clear();
        this.createPlot();
    }

    loadString(jsonString) {
        this.setOptionsForAlignmentView();
        this.data = new LCAlignmentData(JSON.parse(jsonString));
        this.clear();
        this.createPlot();
    }

    loadStringForCompound(jsonString, mainFeature) {
        this.setOptionsForAdductView();
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

    loadData(dataUrl, afterwards, compoundMainFeature) {
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

        if (this.order == "ALPHABETICALLY_SELECTED_FIRST") { //this is for adduct view, to ensure that correlated traces are sorted behind main feature
            comparisonFunction = (u, v) => {
                if (u.hasSelectedFeature() && !v.hasSelectedFeature()) return -1;
                if (!u.hasSelectedFeature() && v.hasSelectedFeature()) return 1;
                return u.label.localeCompare(v.label);
            };
        }

        this.samples = d3.select(this.options.legendId);
        this.samples.selectAll("ul").remove();
        if (this.options.showLegend) {
            this.items = this.samples.append("ul").classed("lclegend-list", true).selectAll("li").data(this.data.traces).join("li")
                .classed("lclegend-item", true).attr("id", (d)=>`sample-${d.index}`).sort(comparisonFunction);

            this.items.append("span").classed("lccolor-box", true).style("background-color", (d)=>d.color);
            this.items.append("span").classed("lcsample-name", true).text((d)=>d.label);
            this.items.append("span").classed("lcintensity-value", true).text((d)=>intensity(d.relativeMainFeatureIntensity));
            this.items.on("click", (e,d)=>d.mainFeature()===null ? null : this.zoomToFeature(d.mainFeature()));

            this.items.on("mouseover", (e,d)=>this.focusOn(d.index))
                .on("mouseout", (e,d)=>(d.mainFeature()===null) ? null : this.removeFocus(d.mainFeature()));
        }
        this.graphView.clear();
        if (this.options.showAdductGraph) {
            this.graphView.setGraph(this.data.adductGraph);
        }



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
        this.graphView = new AdductGraphView(this.options.adductGraphId);

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
        this.annotations=[];
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
                    .style('stroke-dasharray', tr.strokeDashArray)
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
            .style('stroke-width', '3px');
        }

        // add ms2 annotations
        // add all other traces
        for (var i=0; i < this.data.traces.length; ++i) {
            let tr = this.data.traces[i];
            let j=i;
            if (tr.ms2Annotations.length>0) {
                let ano = this.mainPlot.append("circle").data(tr.ms2Annotations).attr("class","ms2ano").attr("cx", (d)=>this.xScale(d.rt))
                            .attr("cy",(d)=>this.yScale(d.intensity)).attr("r",5).attr("stroke", tr.color);
                this.annotations.push(ano);
            }

        }

        // add event listeners
        if (this.options.showAdductGraph) {
            this.graphView.onTraceInFocus((idx)=>this.focusOn(idx));
            this.graphView.onTraceOutOfFocus((idx)=>this.removeFocus());
        }
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
            .attr('class', "magnifier")
            .attr("cx", 11)
            .attr("cy", 11)
            .attr("r", 8)
            .attr("fill", "none")

        // Draw the horizontal line (minus sign)
        this.zoomOut.append("line")
            .attr('class', "magnifier")
            .attr("x1", 7)
            .attr("y1", 11)
            .attr("x2", 15)
            .attr("y2", 11)

        // Draw the handle of the magnifying glass
        this.zoomOut.append("line")
            .attr('class', "magnifier")
            .attr("x1", 16.6569)
            .attr("y1", 16.6569)
            .attr("x2", 23)
            .attr("y2", 23)


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
        const buffer = (feature.toRt-feature.fromRt)*0.33;
        const from = Math.max(feature.fromRt-buffer, this.xScale.domain()[0]);
        const to = Math.min(feature.toRt+buffer, this.xScale.domain()[1]);

        const newXDomain = [from, to];
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

            // Update anos
            transition.selectAll('.ms2ano')
                .attr('cx', d => newXScale(d.rt)).attr("cy", d=>newYScale(d.intensity));

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
        this.adductGraph = null;
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
            const tid = this.json.traces[i].id;
            if (tid===this.mainFeatureId) {
                this.setSpecialTrace(tr);
            }
        }
        this.finishDefinition();
        if (this.specialTrace) {
            for (var l=0; l < this.specialTrace.featureAnnotations.length; ++l) {
                this.addFocusFeature(this.specialTrace.featureAnnotations[l]);
            }
        }
        if (json.adductNetwork) {
            this.adductGraph = new AdductGraph(this, mainFeature);
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
        this.color = index >= 0 ? (json.color != null ? json.color : getColor(index)) : null;
        this.strokeDashArray = json.dashStyle ? json.dashStyle : 'none';
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
        this.mainFeatureIdx = -1;
        if (json.annotations) {
            for (var i=0; i < json.annotations.length; ++i) {
                let a = json.annotations[i];
                if (a.type=="FEATURE") {
                    var d = new DataSelection(
                        a.index, a.from, a.to, rts[a.index], rts[a.from], rts[a.to], a.description
                    );
                    this.featureAnnotations.push(d);
                    if (d.isMainFeature()) {
                        this.mainFeatureIdx = this.featureAnnotations.length-1;
                    }
                } else if (a.type=="MS2") {
                    this.ms2Annotations.push(this.data_[a.index]);
                }
            }
        }
    }

    getMainFeatureIntensity() {
        return this.featureIntensity(); // currently this is the same
    }

    mainFeature() {
        if (this.mainFeatureIdx>=0) return this.featureAnnotations[this.mainFeatureIdx];
        else return null;
    }

    featureIntensity() {
        if (this.mainFeatureIdx < 0) return 0.0;
        let feature = this.mainFeature();
        return this.data_[feature.apex].intensity;
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

    /*
    it "adduct view" (LCCompoundData) a selected feature's isotope pattern plus correlated features are shown
    if true this trace contains the selected feature or its isotope feature
     */
    hasSelectedFeature() {
        return this.json.label.includes("[SELECTED]")
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
class AdductGraphView {

    constructor(containerId) {
        this.container = d3.select(containerId);
    }

    clear() {
        this.container.selectAll("svg").remove();
        this.container.selectAll("div").remove();
    }

    onTraceInFocus(listener) {
        this.infocusListener.push(listener);
    }
    onTraceOutOfFocus(listener) {
        this.outfocusListener.push(listener);
    }

    setGraph(adductGraph) {
        this.clear();
        this.svg = this.container.append("svg");
        this.svg.classed("lc-adduct-graph", true);
        const vwidth = 600;
        const vheight = 250;
        this.svg.attr("viewBox", "0 0 " + vwidth + " " + vheight);
        this.legend = this.container.append("div");
        this.graph = adductGraph;
        this.infocusListener = [];
        this.outfocusListener = [];
        this.infocusEdgeListener = [];
        this.outfocusEdgeListener = [];

        if (adductGraph==null || adductGraph.nodes.length==0) return;

        const linkGroup = this.svg.append("g")
            .attr("stroke", "#999")
            .attr("stroke-opacity", 0.6);

        const link = linkGroup.selectAll("line")
            .data(adductGraph.links)
            .join("line")
            .attr("stroke-dasharray", function(l){if (l.pvalue > -2) return "4"; else return "none";})
            .attr("stroke-width", function(l){return Math.min(8, Math.abs(l.pvalue))})
            .attr("style", "pointer-events: none");

        const linkLabels = linkGroup.selectAll("text")
            .data(adductGraph.links)
            .join("text")
            .attr("font-size", 10)
            .attr("font-weight", "bold")
            .attr("fill", "#000000")
            .text(d => d.label)
            .on("mouseover", (event,edge)=>{
                for(let i=0; i < this.infocusEdgeListener.length; ++i) this.infocusEdgeListener[i](edge);
            })
            .on("mouseout", (event, edge)=>{
                for (let i=0; i < this.outfocusEdgeListener.length; ++i) this.outfocusEdgeListener[i](edge);
            });

        const nodeGroup = this.svg.append("g");

        const nodes = nodeGroup.selectAll("g")
            .data(adductGraph.nodes)
            .join("g");

        const nodeRects = nodes.append("rect")
            .attr("rx", 10) // Rounded corners
            .attr("ry", 10)
            .attr("fill", (n)=>n.color)
            .attr("stroke", "#fff")
            .attr("stroke-width", 1.5)
            .on("mouseover", (event,node)=>{
                for(let i=0; i < this.infocusListener.length; ++i) this.infocusListener[i](node.traceIndex,node);
            })
            .on("mouseout", (event, node)=>{
                for (let i=0; i < this.outfocusListener.length; ++i) this.outfocusListener[i](node.traceIndex,node);
            });

        const nodeLabels = nodes.append("text")
            .attr("text-anchor", "middle")
            .attr("alignment-baseline", "middle")
            .attr("font-size", 12)
            .attr("fill", "#fff")
            .attr("font-weight", "bold")
            .attr("style", "pointer-events: none")
            .text(d => d.label);

        // Calculate dimensions for rectangles based on text
        nodes.each(function (d) {
            const bbox = this.querySelector("text").getBBox();
            d.width = bbox.width + 10;  // Add padding
            d.height = bbox.height + 10; // Add padding
        });
          function dragstarted(event) {
            if (!event.active) simulation.alphaTarget(0.3).restart();
            event.subject.fx = event.subject.x;
            event.subject.fy = event.subject.y;
          }

          // Update the subject (dragged node) position during drag.
          function dragged(event) {
            const x = Math.min(vwidth-25, Math.max(event.x, 25));
            const y = Math.min(vheight-25, Math.max(event.y, 25));
            event.subject.fx = x;
            event.subject.fy = y;
          }

          // Restore the target alpha so the simulation cools after dragging ends.
          // Unfix the subject position now that it’s no longer being dragged.
          function dragended(event) {
            if (!event.active) simulation.alphaTarget(0);
            //event.subject.fx = null;
            //event.subject.fy = null;
          }
        nodes.call(d3.drag()
        .on("start", dragstarted)
        .on("drag", dragged)
        .on("end", dragended));
        const boundaryForce = function(alpha) {
            const nodes = adductGraph.nodes;
            for (let i = 0, n = nodes.length; i < n; ++i) {
                let node = nodes[i];
                if (Number.isFinite(node.vx) && Number.isFinite(node.vy)) {
                    const border = 25.0;
                    const strength = 40;// * alpha; // Smoothed force
                    const leftForce = (1.0 - Math.min(node.x, border) / border) * strength;
                    const rightForce = -(Math.max(node.x - (vwidth - border), 0) / border) * strength;
                    const upForce = (1.0 - Math.min(node.y, border) / border) * strength;
                    const downForce = -(Math.max(node.y - (vheight - border), 0) / border) * strength;
                    node.vx += leftForce + rightForce;
                    node.vy += downForce + upForce;
                }
            }
        };
        const linkForce = function(alpha) {
            const linkStrength = alpha;
            const edges = adductGraph.links;
            for (let i=0; i < edges.length; ++i) {
                const edge = edges[i];
                const w = Math.abs(edge.source.x-edge.target.x);
                const h = Math.abs(edge.source.y-edge.target.y);
                const minSize = 25+edge.label.length*6;
                {
                    var u,v;
                    if (edge.source.x < edge.target.x) {
                        u = edge.source; v=edge.target;
                    } else {
                        u = edge.target; v = edge.source;
                    }
                    var drift;
                    if (w < minSize && w > h) {
                        drift = -(w-minSize)*linkStrength;
                    } else if (w > minSize) {
                        drift = -Math.sqrt(w-minSize)*linkStrength*0.01;
                    } else drift = 0.0;
                    u.vx -= drift;
                    v.vx += drift;

                }
                {
                    var u,v;
                    if (edge.source.y < edge.target.y) {
                        u = edge.source; v=edge.target;
                    } else {
                        u = edge.target; v = edge.source;
                    }
                    var drift;
                    if (h < minSize && h > w) {
                        drift = -(h-minSize)*linkStrength;
                    } else if (h > minSize) {
                        drift = -Math.sqrt(h-minSize)*linkStrength*0.01;
                    } else drift = 0.0;
                    u.vy -= drift;
                    v.vy += drift;
                }

            }
        }

        const simulation = d3.forceSimulation(adductGraph.nodes)
            .force("charge", d3.forceManyBody().strength(-500)) // Adjusted charge
            .force("link", linkForce) // Stronger links
            .force("center", d3.forceCenter(vwidth / 2, vheight / 2).strength(0.5)) // Dynamic centering
            .force("boundary", boundaryForce) // Boundary enforcement
            .force("collide", d3.forceCollide((d) => d.width * 0.5 + 10)) // Added padding
            /*
            .force("gravity", alpha => {
                adductGraph.nodes.forEach(node => {
                    node.vx -= (node.x - vwidth / 2) * 0.001 * alpha;
                    node.vy -= (node.y - vheight / 2) * 0.001 * alpha;
                })
            })
            */
            .force("x", d3.forceX())
            .force("y", d3.forceY());
        simulation.on("tick", () => {
            // Update links
            link
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);

            // Update link labels
            linkLabels
                .each(d=>{
                    let angle = Math.atan2(d.target.y - d.source.y, d.target.x - d.source.x) * 180 / Math.PI;
                    // text should not be upside down
                    if (angle < 0) angle = 360+angle;
                    if (angle > 90 && angle <= 270) {
                        angle +=180;
                        d.rotated = true;
                    } else d.rotated = false;
                    d.angle = angle;
                    if (d.rotated && d.label.includes(" -> ")) {
                        let [a,b] = d.label.split(" -> ");
                        d.rotatedText = b + " -> " + a;
                    } else {
                        d.rotatedText = d.label;
                    }
                })
                .attr("x", d => (d.source.x + d.target.x) / 2)
                .attr("y", d => (d.source.y + d.target.y) / 2)
                .attr("text-anchor", "middle") // Center the text horizontally
                .attr("dominant-baseline", "central") // Center the text vertically
                .attr("transform", d => {
                    let angle = d.angle;
                    return `rotate(${angle}, ${(d.source.x + d.target.x) / 2}, ${(d.source.y + d.target.y) / 2})`;
                }).text(d=>d.rotatedText);
            // Update node positions
            nodes
                .attr("transform", d => `translate(${d.x},${d.y})`);

            // Update rectangle sizes and positions
            nodeRects
                .attr("x", d => -d.width / 2)
                .attr("y", d => -d.height / 2)
                .attr("width", d => d.width)
                .attr("height", d => d.height);

            // Node labels are automatically centered by `text-anchor` and `alignment-baseline`
        });

        this.infocusListener.push((idx,node)=>{
            this.setTextForNode(node);
        });
        this.outfocusListener.push((idx,node)=>this.clearText());
        this.infocusEdgeListener.push((edge)=>this.setTextForEdge(edge));
        this.outfocusEdgeListener.push((edge)=>this.clearText());

    }

    clearText() {
        this.legend.selectAll("dl").remove();
    }

    setTextForEdge(edge) {
        this.clearText();
        const defin = this.legend.append("dl");
        if (Number.isFinite(edge.scores.mergedCorrelation)) {
            defin.append("dt").text("correlation of merged trace");
            defin.append("dd").text(edge.scores.mergedCorrelation.toPrecision(4));
        }
        if (Number.isFinite(edge.scores.representativeCorrelation)) {
            defin.append("dt").text("correlation of representative trace");
            defin.append("dd").text(edge.scores.representativeCorrelation.toPrecision(4));
        }
        if (Number.isFinite(edge.scores.intensityRatioScore)) {
            defin.append("dt").text("correlation score between samples");
            defin.append("dd").text(edge.scores.intensityRatioScore.toPrecision(4));
        }
        if (Number.isFinite(edge.scores.ms2cosine)) {
            defin.append("dt").text("Cosine similarity between MS/MS spectra");
            defin.append("dd").text(edge.scores.ms2cosine.toPrecision(4));
        }
        if (Number.isFinite(edge.scores.pvalue)) {
            defin.append("dt").text("pvalue for this edge corresponds to an adduct.");
            defin.append("dd").text(edge.scores.pvalue.toPrecision(4));
        }
    }

    setTextForNode(node) {
        this.clearText();
        const defin = this.legend.append("dl");
        for (let i=0; i < node.adducts.length; ++i) {
            const d = node.adducts[i];
            console.log(node);
            console.log(d);
            defin.append("dt").text(d.name);
            defin.append("dd").text(Math.round(100.0*d.probability) + " %");
        }
    }
}

class AdductGraph {

    constructor(data, mainFeature) {
        this.json = data.json.adductNetwork;
        if (this.json===null) {
            this.nodes=[];
            this.links=[];
            return;
        }
        const json = this.json;
        const features = {};
        for (var tr = 0; tr < data.traces.length; ++tr) {
            let an  = data.traces[tr].json.annotations;
            for (var i=0; i < an.length; ++i) {
                const f = an[i];
                if (f.type=="FEATURE") {
                    const grps = [];
                    for (let el of f.description.matchAll(/\[[^\]]+\]/g)) {
                        grps.push(el[0]);
                    }
                    const id = f.description.match(/(\d+)$/)[1]
                    grps.push(data.traces[tr].index);
                    features[id] = grps;
                }
            }
        }
        this.nodes = json.nodes.map(function(node,index){
            const f = features[node.alignedFeatureId];
            const type = (f.includes("[ISOTOPES]") ? "isotope" : (node.alignedFeatureId==mainFeature ? "main" : "adduct"));
            const add = [];
            for (let [key,value] of Object.entries(node.adductAnnotations)) {
                add.push({probability: value, name: key});
            }
            return {
                json: node,
                index: index,
                x:0, y:0,
                label: "m/z = " + node.mz.toPrecision(4),
                type: type,
                traceIndex: f[f.length-1],
                color: data.traces[f[f.length-1]].color,
                adducts: add
            };
        });
        let xs = this.nodes;
        this.links = json.edges.map(function(edge,index){
            return {
                json: edge,
                source: xs[edge.from],
                target: xs[edge.to],
                index: index,
                label: edge.annotation,
                pvalue: edge.pvalue,
                scores: {
                    mergedCorrelation: edge.mergedCorrelation,
                    representativeCorrelation: edge.representativeCorrelation,
                    ms2cosine: edge.ms2cosine,
                    intensityRatioScore: edge.intensityRatioScore,
                    pvalue: edge.pvalue
                }
            };
        });
    }

}


//only used as fallback
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
    const opts = new ViewOptions(svgSelector);
    const plot = new LiquidChromatographyPlot(opts);
    if (dataUrl) {
        plot.loadData(dataUrl);
    }
    return plot;
}
document.setDark = setDark;
document.setBright = setBright;
document.drawPlot = drawPlot;

window.ViewOptions = ViewOptions;