from plotly import graph_objects as go
from dash import dcc, html, Input, Output, no_update, Dash, callback
from rdkit import Chem
from rdkit.Chem import Draw, rdMolDescriptors
import pandas as pd
import numpy as np
import json
import sys
import base64


def process_spectra_json_node(jsonRoot):
    spectra_node = jsonRoot['spectra']
    spectra_dict = {}

    for d in spectra_node:
        processed_dict = {'recall': d['recall'], 'weighted_recall': d['weighted_recall']}

        msrd_spectrum = {'annotated_matched': {'mz': [], 'intensity': [], 'atom_indices': []}, 'not_annotated_matched': {'mz': [], 'intensity': []}, 'annotated_not_matched': {'mz': [], 'intensity': [], 'atom_indices': []}, 'not_annotated_not_matched': {'mz': [], 'intensity': []}}
        for peak in d['msrd_spectrum']:
            if peak['isMatched']:
                if len(peak['atom_indices']) > 0:
                    msrd_spectrum['annotated_matched']['mz'].append(peak['mz'])
                    msrd_spectrum['annotated_matched']['intensity'].append(peak['intensity'])
                    msrd_spectrum['annotated_matched']['atom_indices'].append(tuple(peak['atom_indices']))
                else:
                    msrd_spectrum['not_annotated_matched']['mz'].append(peak['mz'])
                    msrd_spectrum['not_annotated_matched']['intensity'].append(peak['intensity'])
            else:
                if len(peak['atom_indices']) > 0:
                    msrd_spectrum['annotated_not_matched']['mz'].append(peak['mz'])
                    msrd_spectrum['annotated_not_matched']['intensity'].append(peak['intensity'])
                    msrd_spectrum['annotated_not_matched']['atom_indices'].append(tuple(peak['atom_indices']))
                else:
                    msrd_spectrum['not_annotated_not_matched']['mz'].append(peak['mz'])
                    msrd_spectrum['not_annotated_not_matched']['intensity'].append(peak['intensity'])

        pred_spectrum = {'mz': [], 'intensity': [], 'atom_indices': []}
        for peak in d['pred_spectrum']:
            pred_spectrum['mz'].append(peak['mz'])
            pred_spectrum['intensity'].append(peak['intensity'])
            pred_spectrum['atom_indices'].append(tuple(peak['atom_indices']))

        processed_dict['msrd_spectrum'] = msrd_spectrum
        processed_dict['pred_spectrum'] = pred_spectrum
        spectra_dict[d['name']] = processed_dict

    return spectra_dict

def get_figure(msrd_spectrum, pred_spectrum, show_legend):
    # 1. Create all trace instances:
    # 1.1 A Scatter instance for the predicted spectrum:
    trace_instances = [go.Scatter(x=pred_spectrum['mz'], y=-np.array(pred_spectrum['intensity']),
                                  error_y=dict(type='data', array=pred_spectrum['intensity'],
                                               arrayminus=[0] * len(pred_spectrum['intensity']), width=0),
                                  customdata=pred_spectrum['atom_indices'], name='predicted spectrum', mode='markers',
                                  marker=dict(color='blue', opacity=0), showlegend=show_legend)]

    # 1.2 4 Scatter instances for the measured spectrum:
    for group, color in color_dict.items():
        trace_instances.append(go.Scatter(x=msrd_spectrum[group]['mz'], y=msrd_spectrum[group]['intensity'],
                                          error_y=dict(type='data', arrayminus=msrd_spectrum[group]['intensity'],
                                                       array=[0] * len(msrd_spectrum[group]['intensity']), width=0),
                                          customdata=msrd_spectrum[group]['atom_indices'] if 'atom_indices' in msrd_spectrum[group].keys() else [None] * len(msrd_spectrum[group]['mz']),
                                          name=f'measured spectrum [{group}]', mode='markers',
                                          marker=dict(color=color, opacity=0), showlegend=show_legend))

    # 1.3 Create the Figure object
    xaxis = go.layout.XAxis(range=(0, rdMolDescriptors.CalcExactMolWt(molecule)), ticks='outside', zeroline=True,
                            zerolinecolor='black', zerolinewidth=1)
    yaxis = go.layout.YAxis(range=(-1.25, 1.25), zeroline=True, zerolinecolor='black', zerolinewidth=1)
    return go.Figure(data=trace_instances, layout=dict(xaxis=xaxis, yaxis=yaxis))


def get_mol_png(smi, atom_indices):
    mol = Chem.MolFromSmiles(smi)
    return Draw._moltoimg(mol, (400, 400), atom_indices, "", returnPNG=True)


# parse the JSON file and extract all data
jsonRoot = json.load(open(sys.argv[1], 'r'))
smiles = jsonRoot['smiles']
molecule = Chem.MolFromSmiles(smiles)
spectra_dict = process_spectra_json_node(jsonRoot)
dropdown_options = list(spectra_dict.keys())

color_dict = dict(annotated_matched='green', not_annotated_matched='orange', annotated_not_matched='purple', not_annotated_not_matched='black')

# create the dash app:
app = Dash(__name__, title='Measured vs. predicted Spectrum')

graph = dcc.Graph(id='mirror_plot', clear_on_unhover=True, config=dict(scrollZoom=True))
dropdown = dcc.Dropdown(dropdown_options, dropdown_options[0], id='dropdown', clearable=False)
epi_fragment_img = html.Img(id='epi_fragment_img', src=f'data:image/png;base64, {base64.b64encode(get_mol_png(smiles, None)).decode("ascii")}',
                            style=dict(top='0px', margin='0px 0px 10px 0px', height=250, width=250, border=2))
pred_fragment_img = html.Img(id='pred_fragment_img', src=f'data:image/png;base64, {base64.b64encode(get_mol_png(smiles, None)).decode("ascii")}',
                             style=dict(bottom='0px', margin='10px 0px 0px 0px', height=250, width=250, border=2))
recall_heading = html.H3(id="recall_heading", style={'font-family': 'sans-serif'})

app.layout = html.Div([
    html.Div([dropdown], style=dict(display='block', width='100%')),
    html.Div([html.Div([graph], style=dict(float='left', width='80%')),
              html.Div([epi_fragment_img, pred_fragment_img], style=dict(float='right', height=500, width='20%'))], style=dict(display='block', margin='20px 20px 10px 30px')),
    html.Div([recall_heading], style=dict(display='block', width='100%'))])


@app.callback(
    Output('mirror_plot', 'figure'),
    Output('recall_heading', 'children'),
    Input('dropdown', 'value'))
def update_figure(data):
    # 1. Create the figure:
    pred_spectrum = spectra_dict[data]['pred_spectrum']
    msrd_spectrum = spectra_dict[data]['msrd_spectrum']
    fig = get_figure(msrd_spectrum, pred_spectrum, show_legend=False)

    # 2. Display the recall and the weighted recall:
    recall_string = f'Recall:\t{spectra_dict[data]["recall"]}\nWeighted recall:\t{spectra_dict[data]["weighted_recall"]}'

    return fig, recall_string


@app.callback(Output('epi_fragment_img', 'src'),
          Output('pred_fragment_img', 'src'),
          Input('mirror_plot', 'hoverData'))
def display_hover(hover_data):
    if hover_data is None:
        return no_update, no_update
    else:
        atom_indices = hover_data['points'][0]['customdata']
        img_data = get_mol_png(smiles, atom_indices)
        src = f'data:image/png;base64, {base64.b64encode(img_data).decode("ascii")}'

        if hover_data['points'][0]['y'] > 0:
            return src, no_update
        else:
            return no_update, src



if __name__ == '__main__':
    app.run()
