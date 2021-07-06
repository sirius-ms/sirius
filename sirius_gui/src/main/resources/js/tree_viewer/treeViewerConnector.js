function requestNewScores(){
    // placeholder for now, until it is decided how to handle scores
}

function getRescoredTree(json_tree){
    return connector.getRescoredTree(json_tree);
}

function formulaDiff(f1, f2){
    return connector.formulaDiff(f1, f2);
}

function formulaIsSubset(f1, f2){
    return connector.formulaIsSubset(f1, f2);
}

function getCommonLosses(){
    if (typeof variable !== 'undefined')
        return connector.getCommonLosses();
    else
        return [];
}

function selectionChanged(mz){
    try {
       connector.selectionChanged(mz);
   } catch (error) {
       console.log(error);
   }
}

function setSelection(mz){
    highlightedNode = d3.selectAll('.node').filter((d, i) => Math.abs(d.data.fragmentData.mz - mz) < 1e-3);
    // blinking for highlighted node
    // TODO: not that pretty, maybe something similar?
    // original_color = highlightedNode.select('rect').style('fill');
    // highlightedNode.select('rect').transition()
    //     .duration(100)
    //     .style('fill', 'white')
    //     .transition()
    //     .duration(100)
    //     .style('fill', original_color);
    // unhighlight any other node and highlight selected node
    for (var style in styles[theme]['node-rect'])
        svg.selectAll('.node rect').style(style, styles[theme]['node-rect'][style]);
    for (var style in styles[theme]['node-rect-selected'])
        highlightedNode.select('rect').style(
            style, styles[theme]['node-rect-selected'][style]);
}

// FUN stuff

function isXmas(){
    return connector.getXmas();
}

function deactivateSpecial(){
    connector.deactivateSpecial();
    d3.selectAll('span').remove();
    reset();
}
