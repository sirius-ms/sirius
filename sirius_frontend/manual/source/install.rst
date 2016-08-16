##############
Installation
##############

User Interface
---------

SIRIUS should hopefully work out of the box. You just have to install a java runtime environment (version >= 7). If you have a 64 bit operating system you also have to install a 64 bit java runtime! 

Windows
---------

The sirius3-console.exe as well as Sirius3-gui.exe should hopefully work out of the box. To execute the SIRIUS commandline tool from every location you have to add the location of the sirius.exe to your PATH environment variable.

Linux and MacOSX
-------------------

With SIRIUS 3.2 we deliver all necessary third-party dependencies with SIRIUS.

To execute SIRIUS from every location you have to add the location of the sirius executable to your PATH variable. Open the file ``~/.bashrc`` in an editor and add the following line (replacing the placeholder path)::

  export PATH=$PATH:/path/to/sirius

Gurobi
-------

SIRIUS ships with the GLPK solver which is fast enough in most cases. However, if you want to analyze large molecules and spectra with lot of peaks, you can greatly improve the running time by using a more efficient solver. Next go GLPK we also support the Gurobi [#gurobi]_ solver. This is a commercial solver which offers a free academic licence for university members. You can find the installation instruction for Gurobi on their website. SIRIUS will automatically use Gurobi als solver if the environment variables for the library path (PATH on windows, LD_LIBRARY_PATH on linux, DYLIB_LIBRARY_PATH on MacOSX) are set to the lib directory of your gurobi installation and if the environment variable GUROBI_HOME is set to your gurobi installation location.
Gurobi will greatly improve the speed of the computation. Beside this there will be no differences in using Gurobi or GLPK.


.. rubric:: Footnotes

.. [#gurobi] http://www.gurobi.com/
