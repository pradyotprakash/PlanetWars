#include <iostream>
#include "PlanetWars.h"

// The DoTurn function is where your code goes. The PlanetWars object contains
// the state of the game, including information about all planets and fleets
// that currently exist. Inside this function, you issue orders using the
// pw.IssueOrder() function. For example, to send 10 ships from planet 3 to
// planet 8, you would say pw.IssueOrder(3, 8, 10).
//
// There is already a basic strategy in place here. You can use it as a
// starting point, or you can throw it out entirely and replace it with your
// own. Check out the tutorials and articles on the contest website at
// http://www.ai-contest.com/resources.
void DoTurn(const PlanetWars& pw) {
  // (1) If we currently have a fleet in flight, just do nothing.
  if (pw.MyFleets().size() >= 1) {
    return;
  }
  // (2) Find my strongest planet.
  int source = -1;
  double source_score = -999999.0;
  int source_num_ships = 0;
  std::vector<Planet> my_planets = pw.MyPlanets();
  for (int i = 0; i < my_planets.size(); ++i) {
    const Planet& p = my_planets[i];
    double score = (double)p.NumShips();
    if (score > source_score) {
      source_score = score;
      source = p.PlanetID();
      source_num_ships = p.NumShips();
    }
  }
	int dest = -1;
	double dest_score = -999999.0;
  // (3) Find the weakest neutral planet, if available.
  if( pw.NeutralPlanets().size() != 0){
	  std::vector<Planet> neutral_planets = pw.NeutralPlanets();
	  for (int i = 0; i < neutral_planets.size(); ++i) {
	    const Planet& p = neutral_planets[i];
	    double score = 1.0 / (1 + p.NumShips());
	    if (score > dest_score) {
	      dest_score = score;
	      dest = p.PlanetID();
	    }
		}
  }
  // else move on to enemies
  else
  {
	  std::vector<Planet> enemy_planets = pw.EnemyPlanets();
	  for (int i = 0; i < enemy_planets.size(); ++i) {
	    const Planet& p = enemy_planets[i];
	    double score = 1.0 / (1 + p.NumShips());
	    if (score > dest_score) {
	      dest_score = score;
	      dest = p.PlanetID();
	    }
		}
 	
  }
  // (4) Send half the ships from my strongest planet to the weakest
  // planet that I do not own.
  if (source >= 0 && dest >= 0) {
    int num_ships = source_num_ships / 2;
    pw.IssueOrder(source, dest, num_ships);
  }
}

// This is just the main game loop that takes care of communicating with the
// game engine for you. You don't have to understand or change the code below.
int main(int argc, char *argv[]) {
  std::string current_line;
  std::string map_data;
  while (true) {
    int c = std::cin.get();
    current_line += (char)c;
    if (c == '\n') {
      if (current_line.length() >= 2 && current_line.substr(0, 2) == "go") {
        PlanetWars pw(map_data);
        map_data = "";
        DoTurn(pw);
	      pw.FinishTurn();
      } else {
        map_data += current_line;
      }
      current_line = "";
    }
  }
  return 0;
}
