
let SCREEN_WIDTH = 1920;
let SCREEN_HEIGHT = 1200;

let PLOT_WIDTH = SCREEN_WIDTH*0.75;
let PLOT_HEIGHT = SCREEN_HEIGHT * 0.5;


document.lcdata = [];
Object.keys(document.traces).forEach(function(traceId) {
  if (traceId >= 0) {
    document.lcdata.push(document.traces[traceId]);
  }
});
document.lcdata.sort(function(a,b){return a.mz - b.mz;});
document.currentIndex = 0
d3.select("#mzslider").node().min = document.lcdata[0].mz;
d3.select("#mzslider").node().max = document.lcdata[document.lcdata.length-1].mz;
d3.select("#mzslider").style("width", (PLOT_WIDTH - 20) + "px");
function binarySearchClosest(arr, target,functor) {
    let low = 0;
    let high = arr.length - 1;
    let closestIndex = -1;

    while (low <= high) {
        const mid = Math.floor((low + high) / 2);
        const guess = functor(arr[mid]);

        if (guess === target) {
            return mid; // Target found, return its index
        } else if (guess < target) {
            low = mid + 1; // Target is in the right half
        } else {
            high = mid - 1; // Target is in the left half
        }

        // Update the closest index if needed
        if (closestIndex === -1 || Math.abs(functor(arr[mid]) - target) < Math.abs(functor(arr[closestIndex]) - target)) {
            closestIndex = mid;
        }
    }

    return closestIndex; // Target not found, return the closest index
}
function updateSlider(evt) {
  let value = d3.select("#mzslider").node().value;
  d3.select("#mznum").node().value = value;
  let index = binarySearchClosest(document.lcdata, parseFloat(value), (x)=>x.mz);
  document.currentIndex = index;
  refreshPlot();
}
function left() {
  document.currentIndex -= 1;
  if (document.currentIndex < 0) {
    document.currentIndex = document.lcdata.length-1;
  }
  d3.select("#mzslider").node().value = document.lcdata[document.currentIndex].mz;
  refreshPlot();
}
function search() {
  let number = parseFloat(d3.select("#mznum").node().value);
  let index = binarySearchClosest(document.lcdata, number, (x)=>x.mz);
  if (Math.abs(document.lcdata[index].mz - number) < 1.5) {
    document.currentIndex = index;
    d3.select("#mzslider").node().value = document.lcdata[document.currentIndex].mz;
    refreshPlot();
  }
  
}
function right() {
  document.currentIndex += 1;
  if (document.currentIndex >= document.lcdata.length) {
    document.currentIndex = 0;
  }
  d3.select("#mzslider").node().value = document.lcdata[document.currentIndex].mz;
  refreshPlot();
}

var selectedIsotope = 1;

function refreshIsobuttons() {
  let fieldset = d3.select("#isotopeSelection");
  let niso = document.lcdata[document.currentIndex].isotopes.length;
  let isoFields=fieldset.html("<input type=\"radio\" id=\"iso0\" name=\"iso0\" value=\"0\"></input><label for=\"iso0\">monoisotope</label>");
  for (var i=0; i < niso; ++i) {
    let name = "iso"+(i+1);
    isoFields.append("input").attr("type","radio").attr("id", name).attr("name","isotopes").attr("value",i+1).attr("onchange","changeIso(this)");
    isoFields.append("label").attr("for",name).html((i+1)+"th isotope");
  }
  if (selectedIsotope > niso) {
    selectedIsotope = niso;
  }
  d3.select("#iso"+selectedIsotope).property("checked", true);
}

function changeIso(evt) {
  let newVal = parseInt(evt.value);
  if (newVal != selectedIsotope) {
    selectedIsotope = newVal;
    refreshPlot();
  }
}

function refreshPlot() {
  refreshIsobuttons();
  show(document.lcdata[document.currentIndex]);
}
document.displayPlot = d3.select("#displayMerged").node().checked;
function changeMergeDisplay(evt) {
  document.displayPlot =evt.checked;
  d3.select("#parentPath").attr("opacity",document.displayPlot ? "100%" : "5%");
}

function show(data) {
  xaxis = data.rt;
  var container = d3.select("#chart")
  // Declare the chart dimensions and margins.
  const width = PLOT_WIDTH;
  const height = PLOT_HEIGHT;
  const marginTop = 20;
  const marginRight = 20;
  const marginBottom = 30;
  const marginLeft = 40;

  function getColor(index) {
    // Calculate hue based on the index
    const hue = (index * 137.508) % 360;

    // Set saturation and lightness to reasonable values
    const saturation = 70;
    const lightness = 60;

    // Convert HSL to RGB using D3.js
    const rgbColor = d3.hsl(hue, saturation, lightness).toString();

    return rgbColor;
}

  var parentMax = 0.0
  for (var i=0; i < data.parent.length; ++i) {
    parentMax = Math.max(parentMax, data.parent[i]);
  }
  const parent = data.rt.map(function(x,i){return {x: x, y: data.parent[i]};});
  const children = Object.keys(data.children).map(function(name,index){
    var normalizer = data.normalization[name];
    let ary = data.children[name];
    return ary.map(function(x,i){return {x: data.rt[i], y: x*normalizer, color: getColor(parseInt(name))};})
  });
  const childrenColors = Object.keys(data.children).map(function(name,index){return getColor(parseInt(name));});
  const noise = data.noise.map(function(x,i){return {x: data.rt[i], y: x};});

  // Declare the x (horizontal position) scale.
  const x = d3.scaleLinear()
      .domain([data.rt[0], data.rt[data.rt.length-1]])
      .range([marginLeft, width - marginRight]);


  // Declare the y (vertical position) scale.
  const y = d3.scaleLinear()
      .domain([data.isotopes.length>0 ? -parentMax : 0, parentMax])
      .range([height - marginBottom, marginTop]);

  // Create the SVG container.
  const svgRoot = d3.create("svg")
      .attr("width", width)
      .attr("height", height);
  const svg = svgRoot.append("g");
  svgRoot.call(d3.zoom()
      .extent([[0, 0], [width, height]])
      .scaleExtent([1, 8])
      .on("zoom", zoomed));

  function zoomed({transform}) {
    svg.attr("transform", transform);
  }


  // Add the x-axis.
  svg.append("g")
      .attr("transform", `translate(0,${height - marginBottom})`)
      .call(d3.axisBottom(x));

  // Add the y-axis.
  svg.append("g")
      .attr("transform", `translate(${marginLeft},0)`)
      .call(d3.axisLeft(y));

  // add the data
  const line = d3.line().x(d=>x(d.x)).y(d=>y(d.y))

  const isoLine = d3.line().x(d=>x(d.x)).y(d=>y(-d.y))
  

  const seg = d3.line().x(d=>d[0]).y(d=>d[1])
  let segs=[]
  const segments = svg; 
  for (var i=0; i < data.segments.length; ++i) {
    let s = data.segments[i];
    let a = data.rt[s.left];
    let b = data.rt[s.right];
    segments.append("rect").attr("x", x(a)).attr("y", y(parentMax)).attr("width", x(b)-x(a)).attr("height", y(0)-y(parentMax)).attr("stroke","black").attr("fill","gray").attr("fill-opacity","5%").classed("region",true);
  }

  // Append a path for the line.
  svg.append("path")
      .attr("id", "noisePath")
      .attr("fill", "none")
      .attr("stroke", "gray")
      .attr("stroke-width", 1.5)
      .attr("stroke-dasharray", 2)
      .attr("d", line(noise));
  svg.append("path")
      .attr("id", "parentPath")
      .attr("fill", "none")
      .attr("stroke", "steelblue")
      .attr("stroke-width", 3)
      .attr("d", line(parent))
      .attr("opacity", document.displayPlot ? "100%" : "5%")
      .classed("parent", true);
  for (i=0; i < children.length; ++i) {
    svg.append("path")
      .attr("fill", "none")
      .attr("stroke", childrenColors[i])
      .attr("stroke-width", 1.5)
      .attr("d", line(children[i]))
      .classed("child", true);
  }

  if (data.isotopes.length>0 && selectedIsotope > 0) {
    let isoData = document.traces[data.isotopes[selectedIsotope-1].toString()];
    console.log(isoData);
    const isoParent = isoData.rt.map(function(x,i){return {x: x, y: isoData.parent[i]};});
    const isoChildren = Object.keys(isoData.children).map(function(name,index){
      var normalizer = isoData.normalization[name];
      let ary = isoData.children[name];
      return ary.map(function(x,i){return {x: isoData.rt[i], y: x*normalizer};})
    });
    const isoChildrenColors = Object.keys(isoData.children).map(function(name,index){return getColor(parseInt(name))});
    console.log((isoParent));
    svg.append("path")
      .attr("id", "parentPath2")
      .attr("fill", "none")
      .attr("stroke", "steelblue")
      .attr("stroke-width", 3)
      .attr("d", isoLine(isoParent))
      .attr("opacity", document.displayPlot ? "100%" : "5%")
      .classed("parent", true);
    for (i=0; i < isoChildren.length; ++i) {
      svg.append("path")
        .attr("fill", "none")
        .attr("stroke", isoChildrenColors[i])
        .attr("stroke-width", 1.5)
        .attr("d", isoLine(isoChildren[i]))
        .classed("child", true);
    }
  }

  d3.select("#mz").html(data.mz.toString());
  d3.select("#rt").html(data.apexRt.toString());


  // Append the SVG element.
  container.html("");
  container.append(()=>svgRoot.node());

}

document.addEventListener("keydown", function(event) {
    // Check if the pressed key is the left arrow key (key code 37)
    if (event.code === "ArrowLeft") {
        left();
    }

    // Check if the pressed key is the right arrow key (key code 39)
    if (event.code === "ArrowRight") {
        right();
    }
});

refreshPlot()