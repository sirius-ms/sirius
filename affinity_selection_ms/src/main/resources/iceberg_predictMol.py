import numpy as np
import json
import sys
import os

import ms_pred.magma.fragmentation as fe
from ms_pred.dag_pred import joint_model

# INITIALISATION:
# Parse the input parameter for predicting the MS2 spectrum:
root_smiles = sys.argv[1]
ionization = sys.argv[2].replace(" ", "")
device = sys.argv[3]
max_nodes = int(sys.argv[4])
threshold = float(sys.argv[5])
binned_out = sys.argv[6] == "true"
models_dir = os.path.abspath(sys.argv[7])
output_dir = os.path.abspath(sys.argv[8])
output_file_name = sys.argv[9]

# Load the ICEBERG-generate and -score models:
gen_ckpt = os.path.join(models_dir, "nist_iceberg_generate.ckpt")
inten_ckpt = os.path.join(models_dir, "nist_iceberg_score.ckpt")
model = joint_model.JointModel.from_checkpoints(gen_checkpoint=gen_ckpt, inten_checkpoint=inten_ckpt)

# PREDICTION:
# Predict the fragmentation process and the fragment intensities:
output = model.predict_mol(smi=root_smiles, adduct=ionization, device=device, max_nodes=max_nodes, threshold=threshold, binned_out=binned_out)
frags = output['frags']

# POST-PROCESSING:
# For each predicted fragment, compute the indices of the present atoms. Add these indices to the corresponding dict.
fragmentation_engine = fe.FragmentEngine(mol_str=root_smiles)
for key in frags.keys():
    frag_bin_repr = frags[key]['frag']
    frags[key]['atom_indices'], _ = fragmentation_engine.get_present_atoms(frag_bin_repr)

# Return frags dictionary as a JSON string to the standard output:
with open(os.path.join(output_dir, output_file_name), 'w') as file_writer:
    file_writer.write(json.dumps(frags))