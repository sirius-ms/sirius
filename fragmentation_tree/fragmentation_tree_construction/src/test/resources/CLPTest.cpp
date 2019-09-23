#define CATCH_CONFIG_MAIN
#include "CLPModel.hpp"
#include "catch.hpp"

#include <vector>

TEST_CASE("Computing ILP Model") {
  CLPModel model{2};
  double* objective = new double[2]{6.0, 7.0};
  model.setObjective(objective, 2);
  double infty = model.getInfinity();
  double* col_lb = new double[2]{0.0, 0.0};
  double* col_ub = new double[2]{infty, infty};
  model.setColBounds(col_lb, col_ub, 2);
  // add rows
  std::vector<double> row1{4.0, 5.0};
  model.addRow(&row1[0], 2, -infty, 20.0);
  std::vector<double> row2{10.0, 7.0};
  model.addRow(&row2[0], 2, -infty, 35.0);
  std::vector<double> row3{3.0, 4.0};
  model.addRow(&row3[0], 2, 6.0, infty);
  // solve
  REQUIRE(model.solve() == CLPModel::RET_OPTIMAL);
  REQUIRE(model.getScore() == Approx(28.0));
  const double* col_solution = model.getColSolution();
  REQUIRE(col_solution[0] == Approx(0.0));
  REQUIRE(col_solution[1] == Approx(4.0));
  delete[] objective;
  delete[] col_lb;
  delete[] col_ub;
}
