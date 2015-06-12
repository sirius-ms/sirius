##############
Installation
##############

Windows
---------

The sirius.exe should hopefully work out of the box. To execute SIRIUS from every location you have to add the location of the sirius.exe to your PATH environment variable.

Linux and MacOSX
-------------------

To execute SIRIUS from every location you have to add the location of the sirius executable to your PATH variable. Open the file ``~/.bashrc`` in an editor and add the following line (replacing the placeholder path)::

  export PATH=$PATH:/path/to/sirius

SIRIUS need an ilp solver to analyze MS/MS data. You can install the free available GLPK solver, e.g. for Ubuntu::

  sudo apt-get install libglpk libglpk-java

Alternatively, SIRIUS ships with the necessary binaries. You might have to add the location of sirius to your LD_LIBRARY_PATH variable (in linux) or to your DYLIB_LIBRARY_PATH variable (MacOsx). For example:

  export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/path/to/sirius

However, it might be that libglpk needs further dependencies, so installing GLPK via package manager is recommended.

Gurobi
-------

SIRIUS ships with the GLPK solver which is fast enough in most cases. However, if you want to analyze large molecules and spectra with lot of peaks, you can greatly improve the running time by using a more efficient solver. Next go GLPK we also support the Gurobi [#gurobi]_ solver. This is a commercial solver which offers a free academic licence for university members. You can find the installation instruction for Gurobi on their website. SIRIUS will automatically use Gurobi als solver if the environment variables for the library path (PATH on windows, LD_LIBRARY_PATH on linux, DYLIB_LIBRARY_PATH on MacOSX) are set to the lib directory of your gurobi installation and if the environment variable GUROBI_HOME is set to your gurobi installation location.
Gurobi will greatly improve the speed of the computation. Beside this there will be no differences in using Gurobi or GLPK.


.. rubric:: Footnotes

.. [#gurobi] http://www.gurobi.com/
