#include "CLPModel.hpp"
#include "ClpSimplex.hpp"

#include <cassert>
#include <iostream>
#include <vector>
#include "CoinPackedMatrix.hpp"
#include "CoinPackedVector.hpp"
#include "OsiClpSolverInterface.hpp"

CLPModel::CLPModel(int ncols, ObjectiveSense obj_sense)
    : m_ncols{ncols}, m_nrows{0}, m_obj_sense{obj_sense} {
  assert(m_ncols > 0 && "The model needs to have at least one parameter");
  m_si = new OsiClpSolverInterface;
  // NOTE: no logs
  m_si->setLogLevel(0);
  m_matrix = new CoinPackedMatrix(false, 0, 0);
}

CLPModel::~CLPModel() {
  delete m_si;
  delete m_matrix;
  delete[] m_indices;
}

void CLPModel::setObjective(const double objective[], int len) {
  assert(len == m_ncols);
  m_objective = objective;
}

void CLPModel::setColBounds(const double col_lb[], const double col_ub[],
                            int len) {
  assert(len == m_ncols);
  m_col_lb = col_lb;
  m_col_ub = col_ub;
}

void CLPModel::setColStart(const double start[], int len) {
  assert(len == m_ncols);
  m_col_start = start;
}

void CLPModel::addRow(const double row[], int len, double lb, double ub) {
  assert(len == m_ncols);
  if (!m_indices){
    m_indices = new int[m_ncols];
    for (int i{0}; i < m_ncols; ++i) m_indices[i] = i;
  }
  addSparseRow(row, m_indices, len, lb, ub);
}

void CLPModel::addSparseRow(const double elems[], const int indices[], int len,
                            double lb, double ub) {
  m_matrix->appendRow(m_ncols, indices, elems);
  m_row_lb.push_back(lb);
  m_row_ub.push_back(ub);
  ++m_nrows;
}

std::ostream &operator<<(std::ostream &out, const CLPModel &model) {
  if (model.m_si->isProvenOptimal()) {
    out << "solved (optimal) model with score " << model.getScore()
        << " and column solution: ";
    const double *colSolution = model.getColSolution();
    for (int i{0}; i < model.m_ncols; ++i) out << colSolution[i] << " ";
  } else {
    out << "model either not solved or solution not optimal; "
        << "info: #cols=" << model.m_ncols << ", #rows=" << model.m_nrows;
  }
  return out;
}

CLPModel::ReturnStatus CLPModel::solve() {
  // no logging
  m_si->loadProblem(*m_matrix, m_col_lb, m_col_ub, m_objective, &m_row_lb[0],
                    &m_row_ub[0]);
  // set options
  m_si->setObjSense(OBJ_MAXIMIZE);
  for (int i{0}; i < m_ncols; ++i)
    m_si->setInteger(i);  // all variables are integers
  // set col start
  if (m_col_start)
    m_si->setColSolution(m_col_start);
  // TODO: should this be the default/only option?
  m_si->branchAndBound();
  // TODO: can multiple of these be true? -> order
  // TODO: Primal vs Dual?
  if (m_si->isProvenOptimal()) return CLPModel::RET_OPTIMAL;
  if (m_si->isProvenPrimalInfeasible() || m_si->isProvenDualInfeasible())
    return CLPModel::RET_INFEASIBLE;
  if (m_si->isAbandoned()) return CLPModel::RET_ABANDONED;
  if (m_si->isPrimalObjectiveLimitReached() ||
      m_si->isDualObjectiveLimitReached() || m_si->isIterationLimitReached())
    return CLPModel::RET_LIMIT_REACHED;
  return CLPModel::RET_UNKNOWN;
}

void CLPModel::debugInfo() {
  printf("#cols: %d, #rows: %d\n", m_si->getNumCols(), m_si->getNumRows());
  printf("columns: \n");
  for (int i{0}; i < m_ncols; ++i)
    printf("\t%d:\t lb: %.0f, ub: %.0f, obj: %.0f\n", i, m_si->getColLower()[i],
           m_si->getColUpper()[i], m_si->getObjCoefficients()[i]);
  printf("rows: \n");
  for (int i{0}; i < m_nrows; ++i)
    printf("\t%d:\t lb: %.0f, ub: %.0f\n", i, m_si->getRowLower()[i],
           m_si->getRowUpper()[i]);
}
