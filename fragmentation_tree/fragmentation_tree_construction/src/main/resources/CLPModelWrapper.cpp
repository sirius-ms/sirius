#include "CLPModel.hpp"

#ifdef _WIN32
#define DLL_EXPORT __declspec(dllexport)
#else
#define DLL_EXPORT
#endif

extern "C" DLL_EXPORT CLPModel *CLPModel_ctor(int ncols, int obj_sense);
CLPModel *CLPModel_ctor(int ncols,
                        int obj_sense) {  // -1: maximize, 1: minimize
  return new CLPModel(ncols, static_cast<CLPModel::ObjectiveSense>(obj_sense));
}

extern "C" DLL_EXPORT void CLPModel_dtor(CLPModel *self);
void CLPModel_dtor(CLPModel *self) { delete self; }

extern "C" DLL_EXPORT double CLPModel_getInfinity(const CLPModel *self) {
  return self->getInfinity();
}

extern "C" DLL_EXPORT void CLPModel_setObjective(CLPModel *self,
                                                 const double objective[],
                                                 int len) {
  self->setObjective(objective, len);
}

extern "C" DLL_EXPORT void CLPModel_setColBounds(CLPModel *self,
                                                 const double col_lb[],
                                                 const double col_ub[],
                                                 int len) {
  self->setColBounds(col_lb, col_ub, len);
}

extern "C" DLL_EXPORT void CLPModel_setColStart(CLPModel *self,
                                                const double start[], int len) {
  self->setColStart(start, len);
}

extern "C" DLL_EXPORT void CLPModel_addRow(CLPModel *self, const double row[],
                                           int len, double lb, double ub) {
  self->addRow(row, len, lb, ub);
}
extern "C" DLL_EXPORT void CLPModel_addSparseRow(CLPModel *self,
                                                 const double elems[],
                                                 const int indices[], int len,
                                                 double lb, double ub) {
  self->addSparseRow(elems, indices, len, lb, ub);
}
extern "C" DLL_EXPORT int CLPModel_solve(CLPModel *self) {
  return static_cast<int>(self->solve());
}
extern "C" DLL_EXPORT const double *CLPModel_getColSolution(
    const CLPModel *self) {
  return self->getColSolution();
}
extern "C" DLL_EXPORT double CLPModel_getScore(const CLPModel *self) {
  return self->getScore();
}
