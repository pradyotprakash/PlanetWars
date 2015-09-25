#include <iostream>
#include "PlanetWars.h"
#include <fstream>
#include <cmath>
#include <map>

using namespace std;

typedef map<pair<int, int>, double > mp;

int OWNER_ME = 1;
int OWNER_NEUTRAL = 0;

bool calculatedDistances = false;

mp planetToPlanetDistance;

ofstream out("/home/pradyot/Desktop/log.txt");

void computeDistanceBetweenPlanets(const PlanetWars& pw){
	vector<Planet> allPlanets = pw.Planets();
	
	for(int i=0;i<allPlanets.size(); ++i){
		for(int j=i+1; j<allPlanets.size();++j){
			double dist = sqrt(pow(allPlanets[i].X() - allPlanets[j].X(), 2) + pow(allPlanets[i].Y() - allPlanets[j].Y(), 2));
			planetToPlanetDistance[pair<int, int>(i, j)] = dist;
			planetToPlanetDistance[pair<int, int>(j, i)] = dist;
		}
	}

	calculatedDistances = true;
}

int whichPlanet(int ID, const PlanetWars& pw){
	return pw.GetPlanet(ID).Owner();
}

bool doISendAFleet(const PlanetWars& pw){

}

void Snipe(const PlanetWars& pw) {

	// each enemy fleet should be accounted for





	if(pw.EnemyFleets().size() <1){
		return;
	}

	if(pw.MyFleets().size() > 0 ){
		return;
	}

	map<int, bool> planetCanAttack;

	vector<Planet> myPlanets = pw.MyPlanets();
	vector<Fleet> enemyFleet = pw.EnemyFleets();
	vector<Planet> neutralPlanets = pw.NeutralPlanets();
	
	for (int i = 0; i < myPlanets.size(); ++i) {
		planetCanAttack[myPlanets[i].PlanetID()] = true;
	}

	double dist;
	Planet *source_pt;
	const Planet *target;

	bool canAttack = true, attacked = false;

	while(canAttack && !attacked){
		source_pt = NULL;
		dist = 100000000;
		
		for (int i = 0; i < enemyFleet.size(); ++i){

			int sniperTarget = enemyFleet[i].DestinationPlanet();
			target = &pw.GetPlanet(sniperTarget);
			if(whichPlanet(sniperTarget, pw) == OWNER_NEUTRAL ){
				for(int j=0;j<myPlanets.size();++j){
					if( !planetCanAttack[myPlanets[j].PlanetID()] ) continue;
					double curDist =  planetToPlanetDistance[pair<int, int>(myPlanets[j].PlanetID(), target->PlanetID())];
					if( curDist< dist ){
						dist = curDist;
						source_pt = &myPlanets[j];
					}
				}
			}
		}

		if(source_pt == NULL ){
			canAttack = false;
			break;
		}

		if(enemyFleet[0].TurnsRemaining() < dist){
			
			int num_ships = (enemyFleet[0].NumShips() - target->NumShips())
					+ target->GrowthRate()*((dist) - enemyFleet[0].TurnsRemaining()+5);

					if(num_ships < source_pt->NumShips()){
						pw.IssueOrder(source_pt->PlanetID(), target->PlanetID(), num_ships);
						attacked = true;
					}
					else {
						planetCanAttack[source_pt->PlanetID()] = false;
					}
		}

		else{
			return;
		}
	}
}


void Attack(const PlanetWars& pw){

	if(pw.MyFleets().size() > 0 ){
		return;
	}

	out<<"A"<<endl;
	vector<Planet> enemyPlanets = pw.EnemyPlanets();
	vector<Planet> myPlanets = pw.MyPlanets();

	Planet* weakestEnemyPlanet, *myStrongestPlanet;
	
	// int minStrength = 100000000, maxStrength = 0;
	bool visited = false;
	int minShips = 1000000000;

	if(enemyPlanets.size() == 0)
		return;

	for(int i=0;i<enemyPlanets.size();++i){
		
		for(int j=0;j<myPlanets.size();++j){
			double dist = planetToPlanetDistance[pair<int, int>(enemyPlanets[i].PlanetID(), myPlanets[j].PlanetID())];
			int ships_reqd = 10 + enemyPlanets[i].NumShips() + dist*enemyPlanets[i].GrowthRate();

			if(ships_reqd < minShips && myPlanets[j].NumShips() > ships_reqd){
				visited = true;
				minShips = ships_reqd;
				weakestEnemyPlanet = &enemyPlanets[i];
				myStrongestPlanet = &myPlanets[j];
			}
		}
	}


	// 	if(enemyPlanets[i].NumShips()*(1+enemyPlanets[i].GrowthRate()) < minStrength){
	// 		minStrength = enemyPlanets[i].NumShips();
	// 		weakestEnemyPlanet = &enemyPlanets[i];
	// 	}
	// }

	// // my strongest planet
	// for(int i=0;i<myPlanets.size();++i){
	// 	if(myPlanets[i].NumShips()*(1+myPlanets[i].GrowthRate()) > maxStrength){
	// 		maxStrength = myPlanets[i].NumShips();
	// 		myStrongestPlanet = &myPlanets[i];
	// 	}
	// }

	// double dist = sqrt(pow(weakestEnemyPlanet->X() - myStrongestPlanet->X(), 2) + pow(weakestEnemyPlanet->Y() - myStrongestPlanet->Y(), 2));

	// // calculate how many ships are needed to capture the planet completely
	// int num_ships = weakestEnemyPlanet->NumShips() + weakestEnemyPlanet->GrowthRate()*dist;
	
	if(visited){
		out<<"B"<<endl;
		pw.IssueOrder(myStrongestPlanet->PlanetID(), weakestEnemyPlanet->PlanetID(), minShips);
	}
	else return;

}

void DoTurn(const PlanetWars& pw) {
	if(!calculatedDistances)
		computeDistanceBetweenPlanets(pw);
	
	// Snipe(pw);
	if(pw.EnemyFleets().size() == 0)
		Attack(pw);
	else Snipe(pw);
	

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
