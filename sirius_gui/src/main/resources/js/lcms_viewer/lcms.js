class LiquidChromatographyPlot {
    constructor(svgSelector) {
        this.svgSelector = svgSelector;
        this.svg = d3.select(svgSelector);
        this.width = +this.svg.attr('width');
        this.height = +this.svg.attr('height');
        this.margin = { top: 20, right: 30, bottom: 40, left: 100 };
        this.plotWidth = this.width - this.margin.left - this.margin.right;
        this.plotHeight = this.height - this.margin.top - this.margin.bottom;

        this.initPlot();
        this.initZoom();
    }

    loadJson(json) {
        window.console.log("test");
        window.console.log(json);
        window.console.log(json.traces.length);
        this.data = new LiquidChromatographyData(json);
        this.clear();
        this.createPlot();
        window.console.log(json);
    }

    loadData(dataUrl, afterwards) {
        d3.json(dataUrl).then(data => {
            this.data = new LiquidChromatographyData(data);
            this.clear();
            this.createPlot();
            if (afterwards) {
                afterwards.call();
            }
        }).catch(error => {

        });
    }

    focusOn(index) {
        this.removeFocus();
        this.focusedItem = d3.select(`#sample-${index}`);
        this.focusedCurve = d3.select(`#curve-${index}`);
        this.focusedItem.classed("focused", true);
        this.focusedCurve.classed("focused", true);
    }

    removeFocus() {
        if (this.focusedItem) {
            this.focusedItem.classed("focused",false);
            this.focusedItem = null;
        }
        if (this.focusedCurve) {
            this.focusedCurve.classed("focused",false);
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
        if (false) {
            comparisonFunction = (u,v)=>u.name.localeCompare(v.name);
        }
        if (true) {
            comparisonFunction = (u,v)=>v.relativeIntensity-u.relativeIntensity;
        }
        this.samples = d3.select("#legend-container");
        this.samples.selectAll("ul").remove();
        this.items = this.samples.append("ul").classed("legend-list", true).selectAll("li").data(this.data.samples).join("li")
            .classed("legend-item", true).attr("id", (d)=>`sample-${d.index}`).sort(comparisonFunction);

        this.items.append("span").classed("color-box", true).style("background-color", (d)=>getColor(d.index));
        this.items.append("span").classed("sample-name", true).text((d)=>d.name);
        this.items.append("span").classed("intensity-value", true).text((d)=>intensity(d.relativeIntensity));

        this.items.on("mouseover", (e,d)=>this.focusOn(d.index))
            .on("mouseout", (e,d)=>this.removeFocus());

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

        this.svg.append("text").attr("class", "x-label").attr("x", this.plotWidth/2).attr("y",this.height).text("retention time (s)");
        this.svg.append("text").attr("class", "y-label").attr("transform", `translate(10, ${this.height/2}) rotate(-90)`).text("intensity");
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
        this.plotArea.select('.line').attr('d', this.line);
        this.updatePlot();
    }

    createPlot() {
        this.updateSamples();
        if (this.zoomOut) this.zoomOut.remove();
        this.zoomOut = this.plotArea.append("g")
        .attr("transform", "translate(10,5)")
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
        this.zoomOut.append("rect").attr("x",0).attr("y",0).attr("width",23).attr("height",23).attr("fill","white").attr("fill-opacity","0.0");
        this.zoomOut.append("circle")
            .attr("cx", 11)
            .attr("cy", 11)
            .attr("r", 8)
            .attr("stroke", "black")
            .attr("fill", "none")
            .attr("stroke-width", 2);

        // Draw the horizontal line (minus sign)
        this.zoomOut.append("line")
            .attr("x1", 7)
            .attr("y1", 11)
            .attr("x2", 15)
            .attr("y2", 11)
            .attr("stroke", "black")
            .attr("stroke-width", 2);

        // Draw the handle of the magnifying glass
        this.zoomOut.append("line")
            .attr("x1", 16.6569)
            .attr("y1", 16.6569)
            .attr("x2", 23)
            .attr("y2", 23)
            .attr("stroke", "black")
            .attr("stroke-width", 2);

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
        this.featureArea = this.mainPlot.append("rect")
            .datum(this.data.mainFeature)
            .attr("class", "featureBox")
            .attr("x", (d)=>this.xScale(d.fromRt))
            .attr("y", 0)
            .attr("width", (d)=>this.xScale(d.toRt)-this.xScale(d.fromRt))
            .attr("height", this.plotHeight);

        // Define line generator
        this.line = d3.line()
            .x(d => this.xScale(d.rt))
            .y(d => this.yScale(d.intensity));


        this.traces=[];
        // add all other traces
        for (var i=0; i < this.data.traces.length; ++i) {
            let tr = this.data.traces[i];
            let j=i;
            let trace = this.mainPlot.append('path')
                    .datum(tr.data())
                    .attr('class', "curve lines")
                    .attr('d', this.line)
                    .attr("id", (d)=>`curve-${j}`)
                    .style('fill', 'none')
                    .style('stroke', getColor(j))
                    .on("mouseover", (e,d)=>{this.focusOn(j)})
                    .on("mouseout",(e,d)=>this.removeFocus());
                this.traces.push(trace);
        }

        // Add the line to the plot
        this.mergedTrace = this.mainPlot.append('path')
            .datum(this.data.merged.data())
            .attr('class', "maincurve lines")
            .attr('d', this.line)
            .style('fill', 'none')
            .style('stroke', 'black')
            .style('stroke-width', '3px');

        // add event listeners
        this.featureArea.on('click', () => this.zoomToFeature());

    }

    resetZoom() {
        // Create a transition
        this.svg.transition().duration(750).call(
            this.zoomTo(this.xScale, this.yScale)
        );
        this.zoomOut.attr("visibility", "hidden");
    }

    zoomToFeature() {
        // Get the feature's data
        const feature = this.data.mainFeature;

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
            transition.selectAll('.lines')
                .attr('d', d => this.line.x(d => newXScale(d.rt)).y(d => newYScale(d.intensity))(d));

            // Update feature area
            transition.select('.featureBox')
                .attr("x", (d) => newXScale(d.fromRt))
                .attr("width", (d) => newXScale(d.toRt) - newXScale(d.fromRt));
        };
    }   

}


class LiquidChromatographyData {

    constructor(json) {
        this.json = json;
        this.traces = [];
        this.samples = [];
        if (this.json.traces) {
            this.empty=false;
            for (var i=0; i < this.json.traces.length; ++i) {
                if (this.json.traces[i].merged===true) {
                    this.merged = new Trace(this, this.json.traces[i]);
                } else {
                    this.traces.push(new Trace(this, this.json.traces[i]));
                    this.samples.push(new Sample(this, this.json.traces[i].sampleName, i));
                }
            }
            this.empty=false;
            this.mainFeature = this.merged.featureAnnotations[0];
            let maxInt = 0.0;
            for (var i=0; i < this.samples.length; ++i) {
                this.samples[i].intensity = this.traces[i].featureIntensity();
                maxInt = Math.max(maxInt, this.samples[i].intensity);
            }
            for (var i=0; i < this.samples.length; ++i) {
                this.samples[i].relativeIntensity = this.samples[i].intensity/maxInt;
            }
        } else {
            this.empty=true;
            this.traces=[];
            this.samples=[];
            this.mergedTrace=null;
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

    averageNormFactor() {
        if (this.empty) return 0;
        let normFactor = 0; let count=0;
        for (var i=0; i < this.json.traces.length; ++i) {
            let t = this.json.traces[i];
            if (t.merged !== true) {
                normFactor += t.normalizationFactor;
                count += 1;
            }
        }
        return normFactor/count;
    }
    sumNormFactor() {
        if (this.empty) return 0;
        let normFactor = 0; let count=0;
        for (var i=0; i < this.json.traces.length; ++i) {
            let t = this.json.traces[i];
            if (t.merged !== true) {
                normFactor += t.normalizationFactor;
                count += 1;
            }
        }
        return normFactor;
    }

}

class Sample {
    constructor(traceset, name, index) {
        this.traceset = traceset;
        this.name = name;
        this.index = index;
    }
}

class Trace {

    constructor(traceset, json) {
        this.json = json;
        this.traceset = traceset;
        let norm = traceset.sumNormFactor();
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
        this.ms2Annotations = []
        this.featureAnnotations = []
        for (var i=0; i < json.annotations.length; ++i) {
            let a = json.annotations[i];
            if (a.type=="FEATURE") {
                this.featureAnnotations.push(new DataSelection(
                    a.index, rts[a.index], a.from, a.to, rts[a.from], rts[a.to], a.description
                ));
            } else if (a.type=="MS2") {

            }
        }
    }

    featureIntensity() {
        let feature = this.featureAnnotations[0];
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
        this.label = label;
    }
}

function getColor(index) {
    const colorPalette = [
        '#1f78b4', // blue
        '#33a02c', // green
        '#e31a1c', // red
        '#ff7f00', // orange
        '#6a3d9a', // purple
        '#b15928', // brown
        '#a6cee3', // light blue
        '#b2df8a', // light green
        '#fb9a99', // light red
        '#fdbf6f', // light orange
        '#cab2d6', // light purple
        '#ffff99'  // yellow
    ];

    const baseColor = colorPalette[index % colorPalette.length];
    const variationFactor = Math.floor(index / colorPalette.length);

    return adjustColor(baseColor, variationFactor);
}

function adjustColor(color, factor) {
    // Convert hex color to RGB
    const rgb = hexToRgb(color);
    
    // Apply a variation factor
    const r = (rgb.r + factor * 10) % 256;
    const g = (rgb.g + factor * 10) % 256;
    const b = (rgb.b + factor * 10) % 256;

    // Convert back to hex
    return rgbToHex(r, g, b);
}

function hexToRgb(hex) {
    const bigint = parseInt(hex.slice(1), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;

    return { r, g, b };
}

function rgbToHex(r, g, b) {
    return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1).toUpperCase();
}


function drawPlot(svgSelector, dataUrl) {
    const plot = new LiquidChromatographyPlot(svgSelector);
    if (dataUrl) {
        plot.loadData(dataUrl);
    }
    return plot;
}

document.drawPlot = drawPlot;