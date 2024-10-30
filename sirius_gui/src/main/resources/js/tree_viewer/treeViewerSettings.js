// to be loaded on top of treeViewer.js to handle configuration
function applySettings(){
    annot_fields = [];
    config.get("nodeAnnotations").forEach(function(annot){
        annot_fields.push(annot.toString());
    });
    popup_annot_fields = [];
    config.get("popupAnnotations").forEach(function(annot){
        popup_annot_fields.push(annot.toString());
    });
    color_variant = config.get("colorVariant").toString();
    color_scheme = config.get((color_variant == "md_mz" ||
                                 color_variant == "md_ppm")?"colorScheme3"
                              :"colorScheme2").toString();
    show_edge_labels = config.get("edgeLabels");
    show_node_labels = config.get("nodeLabels");
    show_color_bar = config.get("colorBar");
    loss_colors = config.get("lossColors");
    deviation_colors = config.get("deviationColors");
    centered_node_labels = config.get("centeredNodeLabels");
    edit_mode = config.get("editMode");
    switch (config.get("edgeLabelMode")){
    case "angled":
        edge_labels_angled = true;
        edge_label_boxes = false;
        break;
    case "boxed":
        edge_labels_angled = false;
        edge_label_boxes = true;
        break;
    default:
        edge_labels_angled = false;
        edge_label_boxes = false;
        break;
    }
}

function settingsChanged(){
    applySettings();
    // dumpConfig();
    config.getSettings().forEach(function (setting){
        switch (setting.toString()){
        case "colorVariant":
        case "colorScheme2":
        case "colorScheme3":
            colorCode(color_variant, color_scheme);
            break;
        case "colorBar":
            toggleColorBar(show_color_bar);
            break;
        case "nodeAnnotations":
            drawTree();
            break;
        case "popupAnnotations":
            break;
        case "edgeLabels":
            toggleEdgeLabels(show_edge_labels);
            break;
        case "nodeLabels":
            toggleNodeLabels(show_node_labels);
            break;
        case "edgeLabelMode":
            drawLinks(root);
            break;
        case "lossColors":
            drawLinks(root);
            break;
        case "deviationColors":
            drawNodeAnnots(config.get("colorScheme3"));
            break;
        case "centeredNodeLabels":
            drawNodes(root);
        default:
            break;              // do nothing
        };
    });
}

function dumpConfig(){
    console_p.text(
        "colorVariant: " + config.get("colorVariant") + "\n" +
            "colorScheme2: " + config.get("colorScheme2") + "\n" +
            "colorScheme3: " + config.get("colorScheme3") + "\n" +
            "colorBar: " + config.get("colorBar") + "\n" +
            "nodeAnnotations: " + config.get("nodeAnnotations") + "\n" +
            "popupAnnotations: " + config.get("popupAnnotations"), + "\n" +
            "edgeLabels: " + config.get("edgeLabels") + "\n" +
            "nodeLabels: " + config.get("nodeLabels") + "\n" +
            "edgeLabelMode: " + config.get("edgeLabelMode")
    );
}
