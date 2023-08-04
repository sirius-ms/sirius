from rdkit import Chem
from rdkit.Chem import Draw, PandasTools
from rdkit.Chem.Draw.rdMolDraw2D import MolDraw2DCairo, PrepareMolForDrawing
from plotly import graph_objects as pgo
import sys
import math
import base64
from dash import Dash, html, dcc, callback, Output, Input, no_update

def parse_spectrum_string(spec_str): # e.g.: "[(mass_1,intensity_1,atom_indices_1);...;(mass_k,intensity_k,atom_indices_k)]"
	spec_str = spec_str.strip('[]')
	a = spec_str.split(';')
	return [peak_string_to_tuple(p) for p in a]

def peak_string_to_tuple(peak_str):
	peak_str = peak_str.strip('()')
	a = peak_str.split(',')
	a[2] = a[2].split(" ")

	atom_indices = []
	for x in a[2]:
		if x != '':
			atom_indices.append(int(x))
	return (float(a[0]),float(a[1]), atom_indices)

def get_mol_png(smiles, atom_indices):
	mol = Chem.MolFromSmiles(smiles)
	return Draw._moltoimg(mol, (200,200), atom_indices, "", returnPNG=True)


# default arguments:
#sys.argv.extend(["[(51.24315643310547,0.026084871240998633,);(51.24560546875,0.014612722742171567,);(64.19293975830078,0.012850215792106837,);(110.75181579589844,0.012811303089200423,);(225.90908813476562,0.03910967882345035,);(227.02728271484375,1.0,);(227.0465545654297,0.021369411376867486,);(227.9073028564453,0.015970165047692015,);(297.1059875488281,0.12224850880724634,);(327.4095153808594,0.016417402299476046,);(346.1009826660156,0.20419042432659282,);(374.097412109375,0.07851772226925638,);(382.5221862792969,0.013789834198516952,);(391.122314453125,0.6686934488141586,);(416.17987060546875,0.054078983608226154,);(444.17816162109375,0.015652574502758858,);(460.949951171875,0.032536025845972844,);(461.2010498046875,0.6040165950130287,)]", "[(17.02600051609054,1.0,25);(32.979347452090536,1.0,30);(72.09335180409053,1.0,0 1 2 3 4);(104.02566616809054,1.0,6 7 8 9 10 11 12 13);(110.00589654809053,1.0,26 27 28 29 30 31 32);(133.06479132809054,1.0,15 16 17 18 19 20 21 22 23 24);(148.07569036009053,1.0,14 15 16 17 18 19 20 21 22 23 24);(149.08351539209053,1.0,15 16 17 18 19 20 21 22 23 24 25);(164.09441442409053,1.0,14 15 16 17 18 19 20 21 22 23 24 25);(213.02428626409053,1.0,6 7 8 9 10 11 12 13 26 27 28 29 30 31 32);(227.02736026409053,1.0,5 6 7 8 9 10 11 12 13 26 27 28 29 30 31 32);(251.09408007609053,1.0,6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24);(266.1413646160905,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 26 27 28 29 31 32);(267.11280414009053,1.0,6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25);(298.11343561609056,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 26 27 28 29 30 31 32);(313.12433464809055,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 26 27 28 29 30 31 32);(352.2019534920906,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25);(358.14242723609055,1.0,5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 31 32);(360.09270017209053,1.0,6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 26 27 28 29 30 31 32);(374.0957741720906,1.0,5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 26 27 28 29 30 31 32);(376.11142423609056,1.0,6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32);(390.1144982360905,1.0,5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32);(413.20977852409055,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 26 27 28 29 31 32);(429.2285025880905,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 31 32);(445.1818495240906,1.0,0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 26 27 28 29 30 31 32)]", "CCC(CC)N1C2=C(C=C(C=C2)C(=O)NC(CC3=CC=CC=C3)C(=O)N)N=C1C4=CSC=C4", "Rule based fragmentation"])

# Parse the arguments:
measured_spec = parse_spectrum_string(sys.argv[1])
predicted_spec = parse_spectrum_string(sys.argv[2])
smiles = sys.argv[3]
title = sys.argv[4]

measured_spec_masses = [peak[0] for peak in measured_spec]
measured_spec_intensities = [math.sqrt(peak[1]) for peak in measured_spec]
measured_spec_atom_indices = [peak[2] for peak in measured_spec]

predicted_spec_masses = [peak[0] for peak in predicted_spec]
predicted_spec_intensities = [-math.sqrt(peak[1]) for peak in predicted_spec]
predicted_spec_atom_indices = [peak[2] for peak in predicted_spec]


# Create a mirror plot:
max_x = max(max(measured_spec_masses),max(predicted_spec_masses)) * 1.1
bar_width = max_x * 0.002

measured_spec_bar = pgo.Bar(x=measured_spec_masses, y = measured_spec_intensities, name = "Measured Spectrum", 
	offset = 0, marker = {'color': 'black'}, width = bar_width, customdata = measured_spec_atom_indices)
predicted_spec_bar = pgo.Bar(x = predicted_spec_masses, y = predicted_spec_intensities, name = "Predicted Fragmentation", 
	offset = 0, marker = {'color': 'blue'}, width = bar_width, customdata= predicted_spec_atom_indices)

xaxis = pgo.layout.XAxis(range = (0, max_x), ticks = 'outside', zeroline = True, zerolinecolor = 'black', 
	zerolinewidth = 1)
yaxis = pgo.layout.YAxis(range = (-1.5,1.5), zeroline = True, zerolinecolor = 'black', zerolinewidth = 1)

fig = pgo.Figure(layout = dict(xaxis = xaxis, yaxis = yaxis))
fig.add_trace(measured_spec_bar)
fig.add_trace(predicted_spec_bar)
fig.update_traces(hoverinfo='none', hovertemplate=None)

app = Dash(__name__)
app.layout = html.Div(children = [html.H4(children=title),
	dcc.Graph(figure = fig, id = 'graph', clear_on_unhover=True),
	dcc.Tooltip(id = 'tooltip')])


@app.callback(Output('tooltip', 'show'),
			  Output('tooltip', 'bbox'),
			  Output('tooltip', 'children'),
			  Input('graph', 'hoverData'))
def display_hover_data(hoverData):
	if hoverData is None:
		return False, no_update
	else:
		pt = hoverData['points'][0]
		bbox = pt['bbox']
		atom_indices = pt['customdata']
		img_data = get_mol_png(smiles, atom_indices)
		return True, bbox, [html.Div([html.Img(src = f'data:image/png;base64, {base64.b64encode(img_data).decode("ascii")}',
			style=dict(float='left', margin='0px 15px 15px 0px', height=200, width=200, border=2))])]

app.run()

