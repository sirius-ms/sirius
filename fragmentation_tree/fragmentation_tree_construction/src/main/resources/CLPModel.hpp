#ifndef CLPMODEL_H
#define CLPMODEL_H
#include <iostream>
#include <vector>
#include "ClpSimplex.hpp"
#include "CoinPackedMatrix.hpp"
#include "CoinPackedVector.hpp"
#include "OsiClpSolverInterface.hpp"
class CLPModel {
 public:
  enum ObjectiveSense { OBJ_MAXIMIZE = -1, OBJ_MINIMIZE = 1 };
  enum ReturnStatus {
    RET_OPTIMAL = 0,
    RET_INFEASIBLE = 1,
    RET_ABANDONED = 2,
    RET_LIMIT_REACHED = 3,
    RET_UNKNOWN = 4
  };

 private:
  OsiClpSolverInterface *m_si;
  CoinPackedMatrix *m_matrix;
  const int m_ncols;
  int m_nrows;
  const double *m_objective;
  const double *m_col_lb;
  const double *m_col_ub;
  std::vector<double> m_row_lb;
  std::vector<double> m_row_ub;
  const double *m_col_start = nullptr;
  ObjectiveSense m_obj_sense;
  int *m_indices = nullptr;  // NOTE: remove if only sparse rows are needed

 public:
  CLPModel(int ncols, ObjectiveSense obj_sense = OBJ_MAXIMIZE);
  ~CLPModel();
  double getInfinity() const { return m_si->getInfinity(); }
  void setObjective(const double objective[], int len);
  void setColBounds(const double col_lb[], const double col_ub[], int len);
  void setColStart(const double start[], int len);
  void addRow(const double row[], int len, double lb, double ub);
  // NOTE: remove if only sparse rows are needed
  void addSparseRow(const double elems[], const int indices[], int len,
                    double lb, double ub);
  ReturnStatus solve();
  const double *getColSolution() const { return m_si->getColSolution(); }
  double getScore() const { return m_si->getObjValue(); }
  friend std::ostream &operator<<(std::ostream &out, const CLPModel &model);
  void debugInfo();
};

#endif /* CLPMODEL_H */
