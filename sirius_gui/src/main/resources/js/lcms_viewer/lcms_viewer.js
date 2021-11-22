const DROPDOWN = false;
const BLOWUP = 10; 

class LCMSViewer {
    constructor(divId, svgId) {
        this.divId = divId;
        this.svgId = svgId;
        this.data = null;
        this.viewer = null;
        this.index = 0;
        this.dropDown = null;
        this.initialize();
    }

    addDropdownList() {
        if (this.dropDown==null) {
            this.dropDown = d3.select(this.divId).append("select");
        }
    }

    removeDropdownList() {
        if (this.dropDown!=null) {
            this.dropDown.remove();
        }
    }

    clear() {
        this.data = {traceSets: [], sampleNames: [], abundance: []};
    }

    bindStringData(data) {
        this.bind(JSON.parse(data));
    }

    bind(data) {
        this.data = data;
        this.index = 0;
        this.viewer = new SampleViewer(this, this.index);
        this.reloadContent();
    }

    initialize() {
        if(DROPDOWN) this.addDropdownList();
    }

    reloadContent() {
        if (this.viewer) {
            if(DROPDOWN) this.reloadDropDown();
            this.viewer.reload();
        }
    }
    reloadDropDown() {
        let maxAbundance = 0.0;
        for (let i=0; i < this.data.abundance.length; ++i) {
            maxAbundance = Math.max(maxAbundance, this.data.abundance[i]);
        }
        let me = this;
        let name2index = {};
        if (DROPDOWN) {

            if (this.dropDown!=null) {
                this.dropDown.selectChildren().remove();
                for (let i=0; i < this.data.traceSets.length; i++) {
                    const opt = this.dropDown.append("option").attr("value",
                        this.data.sampleNames[i]).text(this.data.sampleNames[i]).append("span").style("font-weight","bold").text(" " + Math.round(this.data.abundance[i]*100.0/maxAbundance) + " %");
                    name2index[this.data.sampleNames[i]] = i;
                }
                this.dropDown.on("change", e=>me.setSample(name2index[e.target.value]));
            }
        } else {
            if (this.data.abundance.length>0) {
                this.setSample(0);
            }
        }
    }

    setSample(index) {
        if (this.index != index) {
            this.index = index;
            this.viewer = new SampleViewer(this, this.index);
            this.viewer.reload();
        }
    }

}

class SampleViewer {

    constructor(lcms, index) {
        this.lcms = lcms;
        this.index = index;
        this.trace = null;
        this.calcAverageRT();
    }

    addDefinitions(svg) {
        svg.append("defs").append("marker").attr("id","arrowhead")
        .attr("markerWidth","10").attr("markerHeight","7").attr("refX","10").attr("refY","3.5").attr("orient","auto")
        .append("polygon").attr("points", "0 0, 10 3.5, 0 7");
    }

    reload() {
        this.trace = this.lcms.data.traceSets[this.index];
        if (!this.trace) {
            d3.select(this.lcms.svgId).html("");
            return;
        }

        const margin = ({
            top: 20,
            right: 20,
            bottom: 30,
            left: 100
        });
        const height = 300;
        const width = 600;

        const traces = this.getTraces();

        const retentionTimes = this.trace.retentionTimes
        const scanIds = this.trace.scanIds;

        const intensityDomain = this.calculateIntensityDomain(traces);

        const x = d3.scaleLinear().domain([retentionTimes[0], retentionTimes[retentionTimes.length - 1]]).nice().range([margin.left, width - margin.right])
        const y = d3.scaleLinear().domain([intensityDomain.min, intensityDomain.max]).nice().range([height - margin.bottom, margin.top]);

        const yAxis = g => g.attr("transform", `translate(${margin.left},0)`)
            .call(d3.axisLeft(y))
        const xAxis = g => g
            .attr("transform", `translate(0,${height - margin.bottom})`)
            .call(d3.axisBottom(x).ticks(width / 80).tickSizeOuter(0));


        const line = d3.line().x((d, i) => x(d.time))
            .y(d => y(d.intensity));

        const svg = d3.select(this.lcms.svgId).html("")
            .attr("viewBox", [0, 0, width, height])
            .style("overflow", "visible");
        this.addDefinitions(svg);
        svg.append("g")
            .call(xAxis);

        svg.append("g")
            .call(yAxis);

        const path = svg.append("g")
            .attr("fill", "none")
            .attr("stroke-width", 1.5)
            //.attr("stroke-linejoin", "round")
            //.attr("stroke-linecap", "round")
            .selectAll("path")
            .data(traces)
            .join("path")
            .attr("stroke", d=>d.color(false,true))
            .attr("id", d => d.name)
            .style("mix-blend-mode", "multiply")
            .attr("d", trace => line(trace.foreground));

        // backgrounds
        const pathBackgroundLeft = svg.append("g")
            .attr("fill", "none")
            .attr("stroke-width", 1)
            .selectAll("path")
            .data(traces)
            .join("path")
            .attr("stroke", d=>d.backgroundColor())
            .attr("stroke-dasharray","4, 5")
            .attr("id", d => d.name + "_backgroundLeft")
            .style("mix-blend-mode", "multiply")
            .attr("d", trace => line(trace.backgroundLeft));
        const pathBackgroundRight = svg.append("g")
            .attr("fill", "none")
            .attr("stroke-width", 1)
            .selectAll("path")
            .data(traces)
            .join("path")
            .attr("stroke", d=>d.backgroundColor())
            .attr("stroke-dasharray","4, 5")
            .attr("id", d => d.name + "_backgroundRight")
            .style("mix-blend-mode", "multiply")
            .attr("d", trace => line(trace.backgroundRight));

        const noiseTrace = this.getNoiseLevel();
        const noisePath = svg.append("g")
            .attr("fill", "none")
            .attr("stroke-width", 1)
            //.attr("stroke-linejoin", "round")
            //.attr("stroke-linecap", "round")
            .selectAll("path")
            .data([noiseTrace])
            .join("path")
            .attr("stroke", "lightgray")
            .attr("id", "noise")
            .attr("stroke-dasharray","4, 5")
            .style("mix-blend-mode", "multiply")
            .attr("d", trace => line(trace));

        const ms2Markers = this.getMs2Markers(traces);
        const ms2MarkerSvg = svg.append("g")
            .attr("stroke-width", 1)
            .selectAll("line")
            .data(ms2Markers)
            .join("line")
            .attr("x1", d=>d.orig(x,y)[0])
            .attr("y1", d=>d.orig(x,y)[1])
            .attr("x2", d=>d.tx(x))
            .attr("y2", d=>d.ty(y))
            .attr("stroke","black")
            .attr("marker-end","url(#arrowhead)");

        // add marker for average and median
        svg.append("g").attr("stroke-width", 1).attr("id","median_avg").selectAll("line")
            .data([this.weightedRetentionTime,this.medianRetentionTime])
            .join("line").attr("x1",d=>x(d)).attr("x2",d=>x(d))
            .attr("y1",d=>y(0)).attr("y2",d=>y(intensityDomain.max)).attr("stroke","lightgray")
            .attr("stroke-dasharray","1, 2")

        const hover = function(svg, path) {

            const ms2Text = svg.append("g")
                .attr("display", "none");
            ms2Text.append("text")
                .attr("font-family", "sans-serif")
                .attr("font-size", 10)
                .attr("text-anchor", "middle")
                .attr("y", -30);

            const left = function() {
                path.style("mix-blend-mode", "multiply").attr("stroke", d=>d.color(false,true));
                dot.attr("display", "none");
            }

            const moved = function(event) {
                event.preventDefault();
                const pointer = d3.pointer(event, this);
                if (pointer[0] < margin.left || pointer[0] > margin.left + width-margin.right || pointer[1] < margin.top || pointer[1] > margin.top+height-margin.bottom ) {
                    left();
                    return;
                } else entered();
                const xm = x.invert(pointer[0]);
                const ym = y.invert(pointer[1]);

                const i = d3.bisectCenter(retentionTimes, xm);
                const inf = 1.0/0.0;
                const s = d3.least(traces, d => 
                    (i >= d.trace.indexOffset && (i-d.trace.indexOffset)<d.trace.masses.length) 
                    ? Math.abs(d.values[i-d.trace.indexOffset].intensity - ym)
                    : inf);
                const K = i - s.trace.indexOffset;
                let text = s.name;
                const noone = s.values[K].background;
                ms2Text.attr("display","none");
                if (s.topTrace) {
                    for (let marker of ms2Markers) {
                        if (((marker.timePoint >= retentionTimes[i]) &&  ((i+1 >= retentionTimes.length) ||
                            marker.timePoint < retentionTimes[i+1])) ||
                            (marker.timePoint <= retentionTimes[i] && (i==0 || marker.timePoint > retentionTimes[i-1]))) {
                            ms2Text.attr("transform", `translate(${x(marker.timePoint)},${y(marker.intensity)})`).attr("display", null);
                            ms2Text.select("text").text("MS/MS scan");
                        }
                    }
                }
                path.attr("stroke", d => d.color(d===s || d.main===s || s.main === d, noone)).filter(d => d === s).raise();
                dot.attr("transform", `translate(${x(retentionTimes[i])},${y(s.values[i-s.trace.indexOffset].intensity)})`);
                dot.select("text").text(s.description(K));
            }

            const entered = function() {
                path.style("mix-blend-mode", null).attr("stroke", d=>d.color(false,true));
                dot.attr("display", null);
            }

            //if ("ontouchstart" in document) {
            //    svg.style("-webkit-tap-highlight-color", "transparent")
            //    .on("touchmove", moved)
            //    .on("touchstart", entered)
            //    .on("touchend", left);
            //} else {
                svg
                .on("mousemove", moved)
                .on("mouseenter", entered)
                .on("mouseleave", left);
            //}

            const dot = svg.append("g")
                .attr("display", "none");

            dot.append("circle")
                .attr("r", 2.5);

            dot.append("text")
                .attr("font-family", "sans-serif")
                .attr("font-size", 10)
                .attr("text-anchor", "middle")
                .attr("y", -8);
        }

        svg.call(hover, path);

        return svg.node();
    }

    calculateIntensityDomain(traces) {
        let min = 1.0 / 0.0;
        let max = -min;
        for (let i = 0; i < traces.length; ++i) {
            const is = traces[i].trace.intensities;
            for (let j = 0; j < is.length; ++j) {
                min = Math.min(min, is[j]);
                max = Math.max(max, is[j]);
            }
        }
        const noise = this.trace["noiseLevels"];
        if (noise) {
            for (let i=0; i < noise.length; ++i) {
                max = Math.max(max, noise[i]*BLOWUP);
            }
        }
        return {
            "min": min,
            "max": max
        };

    }

    calcAverageRT() {
        let avg = 0.0;
        const traces = this.lcms.data.traceSets;
        let weightedRt = 0.0;
        let intSum = 0.0;
        let medianRt = [];
        for (let i=0; i < traces.length; ++i) {
            const ion = traces[i].ionTrace.isotopes[0];
            const is=ion.intensities;
            let maxi=ion.detectedFeatureOffset;
            const n = maxi+ion.detectedFeatureLength;
            for (let j=maxi+1; j < n; ++j) {
                if (is[j]>is[maxi]) {
                    maxi = j;
                }
            }
            const absIndex = maxi+ion.indexOffset;
            intSum += ion.intensities[maxi];
            weightedRt += traces[i].retentionTimes[absIndex]*ion.intensities[maxi];
            medianRt.push(traces[i].retentionTimes[absIndex]);
        }
        medianRt.sort();
        weightedRt /= intSum;
        this.medianRetentionTime = medianRt[medianRt.length>>1];
        this.weightedRetentionTime = weightedRt;
    }

    getMs2Markers(traces) {
        // monoisotopic trace is always the first trace
        const monoIsotopic = traces[0];
        const markers = [];
        const xs = this.trace.ms2RetentionTimes;
        if (xs) {
            for (let i=0; i < xs.length; ++i) {
                const marker = new Marker(monoIsotopic,xs[i], 25);
                marker.ms2ScanId = this.trace.ms2ScanIds[i];
                markers.push(marker);
            } 
        }
        return markers;
    }

    getNoiseLevel() {
        const trace=[];
        const noise = this.trace["noiseLevels"];
        if (noise) {
            for (let i=0; i < noise.length; ++i) {
                trace.push({
                    "intensity": noise[i]*BLOWUP,
                    "time": this.trace.retentionTimes[i]
                });
            }
        }  
        return trace;
    }

    getTraces() {
        const traces = [];
        this.addIsotopeTraces(traces, this.trace["ionTrace"], "ion", 0);
        traces[0].topTrace = true;
        if (this.trace.ionTrace["adducts"]) {
            for (let i = 0; i < this.trace.ionTrace.adducts.length; i++) {
                const mzdiff = (this.trace.ionTrace.adducts[i].isotopes[0].masses[0] - 
                    this.trace.ionTrace.isotopes[0].masses[0]).toFixed(2);
                this.addIsotopeTraces(traces, this.trace.ionTrace.adducts[i], "adduct m/z " + mzdiff, 1);
            }
        }
        if (this.trace.ionTrace["inSourceFragments"]) {
            for (let i = 0; i < this.trace.ionTrace.inSourceFragments.length; i++) {
            	const mz = this.trace.ionTrace.inSourceFragments[i].isotopes[0].masses[0].toFixed(2);
                this.addIsotopeTraces(traces, this.trace.ionTrace.inSourceFragments[i], "in-source m/z " + mz, 2);
            }
        }
        return traces;
    }

    addIsotopeTraces(array, trace, name, style) {
        const monoIsotopic = new Trace(name, trace.isotopes[0], this.trace, style);
        array.push(monoIsotopic);
        for (let i = 1; i < trace.isotopes.length; ++i) {
            const iso = new Trace(name + "+" + i, trace.isotopes[i], this.trace, style);
            monoIsotopic.addSubtrace(iso);
            array.push(iso);
        }
    }


}

class Marker {

    constructor(trace, timePoint, len) {
        this.trace = trace;
        this.timePoint = timePoint;
        // interpolation. Woah, javascript does not have binary search...
        const xs = this.trace.values;
        let before=0, after = 0;
        for (let i=0; i < xs.length; ++i) {
            if (timePoint < xs[i].time) {
                before = i-1 >= 0 ? i-1 : 0;
                after = i;
                break;
            }
        } 
        const ti = xs[before].intensity;
        const tj = xs[after].intensity;
        const delta = xs[after].time - xs[before].time;

        const x = delta == 0 ? 0 : (this.timePoint-xs[before].time) / delta; 
        this.intensity = ti + (tj-ti)*x;
        this.len = len;

        this.dirX = delta;
        this.dirY = tj-ti;
    }

    tx(x) {
        return x(this.timePoint);
    }
    ty(y) {
        return y(this.intensity);
    }
    orig(x,y) {
        const xx = x(this.timePoint);
        const yy = y(this.intensity);
        let dx = x(this.timePoint+this.dirX);
        let dy = y(this.intensity+this.dirY);
        dx -= xx;
        dy -= yy;
        const norm = this.len/Math.sqrt(dx*dx+dy*dy);
        return [xx + dy*norm,yy - dx*norm];
    }



}

const TraceColors = [
    "steelblue", // main ion
    "seagreen", // adducts
    "brown" // in-source fragments
];


class Trace {

    constructor(name, trace, set, style) {
        this.name = name;
        this.trace = trace;
        this.associations = [];
        this.style = style;
        this.main = null;
        this.values = [];
        this.foreground = [];
        this.backgroundRight = [];
        this.backgroundLeft = [];
        this.topTrace = false;
        let left = true;
        for (let i = 0; i < trace.masses.length; ++i) {
            this.values[i] = {
                mz: trace.masses[i],
                intensity: trace.intensities[i],
                time: set.retentionTimes[i + trace.indexOffset],
                scan: set.scanIds[i + trace.indexOffset],
                absolute: i + trace.indexOffset,
                background: i < trace.detectedFeatureOffset || i >= (trace.detectedFeatureOffset + trace.detectedFeatureLength)
            };
            if (this.values[i].background) {
                if (left) this.backgroundLeft.push(this.values[i]);
                else this.backgroundRight.push(this.values[i]);
            } else {
                left = false;
                this.foreground.push(this.values[i]);
            }
        }
        // connect lines
        this.backgroundLeft.push(this.foreground[0]);
        this.backgroundRight.unshift(this.foreground[this.foreground.length-1]);
    }

    description(i) {
        if (this.values[i].background) {
            return "background";
        } else {
            return this.name;
        }
    }

    backgroundColor(hover) {
        return hover ? "black" : "#ddd";
    }

    color(hover, noone) {
        return (hover || (noone && this.main === null)) ? TraceColors[this.style] : "#ddd";
    }

    addSubtrace(trace) {
        this.associations.push(trace);
        trace.associations.push(this);
        trace.main = this;
    }

}