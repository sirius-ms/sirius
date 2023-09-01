from plotly import graph_objects as go 
import pandas as pd
import sys

def assign_peak_to_group(df_row):
	if df_row['matchedMsrdPeak'] == 1:
		return 'matched'
	else:
		if pd.isna(df_row['atomIndices']):
			return 'unknown_fragment'
		else:
			return 'known_fragment'


# 1. Parse the arguments: 
df = pd.read_csv(sys.argv[1])
print(df)
title = sys.argv[2]

measured_spectrum = df.loc[df['type'] == 0]
predicted_spectrum = df.loc[df['type'] == 1]

# 2. Compute the recall and the weighted recall:
matched_peaks = measured_spectrum.loc[measured_spectrum['matchedMsrdPeak'] == 1]
recall = matched_peaks.shape[0] / measured_spectrum.shape[0]
weighted_recall = sum(matched_peaks['intensity']) / sum(measured_spectrum['intensity'])

# 3. Create all scatter plots:
# 3.1: add the predicted spectrum:
trace_instances = []
trace_instances.append(go.Scatter(x=predicted_spectrum['mz'], y=-predicted_spectrum['intensity'], name='predicted',mode='markers', marker=dict(color='blue', opacity=0), 
	error_y=dict(type='data', arrayminus=[0]*predicted_spectrum.shape[0], array=predicted_spectrum['intensity'], width=0)))

# 3.2: add all traces to plot the measured spectrum
# several traces have to be added to assign different colors to the peaks depending if a peak is a TP (orange), FN (black) and annotated by Epimetheus (purple)
measured_spectrum['group'] = [assign_peak_to_group(row) for _,row in measured_spectrum.iterrows()]
group_to_color = dict(matched='orange', unknown_fragment='black', known_fragment='purple')
group_to_trace_name = dict(matched='matched peak', unknown_fragment='not annotated by Epimetheus', known_fragment='annotated by Epimetheus')

for group in ['matched','unknown_fragment','known_fragment']:
	peak_df = measured_spectrum.loc[measured_spectrum['group'] == group]
	trace_instances.append(go.Scatter(x=peak_df['mz'], y=peak_df['intensity'], name=group_to_trace_name[group], mode='markers', marker=dict(color=group_to_color[group], opacity=0), 
		error_y=dict(type='data', array=[0]*peak_df.shape[0], arrayminus=peak_df['intensity'], width=0)))

# 3.3: Specify the layout of the plot 
layout = go.Layout(xaxis=dict(title=dict(text="m/z"), zeroline=True, zerolinecolor='black', linewidth=1, linecolor='black', showgrid=False, mirror=True), 
	yaxis=dict(title=dict(text='Intensity'), range=(-1.25,1.25), zeroline=True, zerolinecolor='black', zerolinewidth=1, linewidth=1, linecolor='black', showgrid=False, mirror=True),
	paper_bgcolor='white', plot_bgcolor='white')

# 3.4: Add the traces and the layout to the figure
fig = go.Figure(data=trace_instances, layout=layout)
fig.show()