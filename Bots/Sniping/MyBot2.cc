#include <iostream>
#include "PlanetWars.h"
#include <fstream>
#include <cmath>
#include <map>
#include <set>
#include "constants.cc"

using namespace std;

typedef map<pair<int, int>, double > mp;

int OWNER_ME = 1;
int OWNER_NEUTRAL = 0;

int w = 0;

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

int fleetScore(const Fleet& f){

	return fleet_ships*f.NumShips() - fleet_turns*f.TurnsRemaining();

}

void Attack(const PlanetWars& pw){

	out << w << endl;
	++w;

	vector<Planet> enemyPlanets = pw.EnemyPlanets();
	vector<Fleet> enemyFleets = pw.EnemyFleets();
	vector<Fleet> myFleets = pw.MyFleets();
	vector<Planet> myPlanets = pw.MyPlanets();

	if(enemyPlanets.size() == 0){
		if(enemyFleets.size() == 0)
			return;
		else{
			// these enemy fleets have not been accounted for
			// similar to sniping, so use sniping wala code here
			return;
		}
	}

	set<pair<int, int>, greater<pair<int, int> > > myPlanetScore;
	set<pair<int, int>, greater<pair<int, int> > > enemyPlanetScore;

	const Planet *enemyWeakest;
	int enemy_score = -1000000007;

	// scores for my planets

	out << 2 <<endl;

	for(int i=0;i<myPlanets.size(); ++i){

		const Planet& currentPlanet = myPlanets[i];

		int heuristic = 0;
		int currentPlanetID = currentPlanet.PlanetID();
		int neighborPlanetID;

		// assume that the heuristic strength of my planet depends on the following factors:
		//   (i) number of ships presently on the planet

		heuristic += planet_ships*currentPlanet.NumShips();
		
		//   (ii) growth rate

		heuristic += planet_growth_rate*currentPlanet.GrowthRate();


		//   (iii) friendly ships present around the planet
		//   		(a) their distance
		//   		(b) their strength
		//   		(c) their growth rate

		//   		In essence, a function of the following form:
		//   			       a                        -----------
		//   			x = ------- + b*strength + c* \/growth rate  
		//  			    distance                  
		
		for(int j=0;j<myPlanets.size();++j){
			
			const Planet& neighbor = myPlanets[j];

			neighborPlanetID = neighbor.PlanetID();

			if(neighbor.PlanetID() != currentPlanet.PlanetID()){
			
				int distance = planetToPlanetDistance[pair<int, int>(currentPlanetID, neighborPlanetID)];
// further change the static parameter distance to a more flexible one which involves the turns
// taken to reach a planet
				if(distance < radius){
					
					// need to consider this planet
					heuristic += friend_distance/distance + friend_ships*neighbor.NumShips() + 
									friend_growth_rate*neighbor.GrowthRate();

				}
			}
		}

		//	(iv) enemy planets present around the planet
		//		a function similar to part (iii) but with different constants
		//		Let's call the value 'y'.
		
		for(int j=0;j<enemyPlanets.size();++j){
			
			const Planet& neighbor = enemyPlanets[j];

			neighborPlanetID = neighbor.PlanetID();

			if(neighborPlanetID != currentPlanetID){
			
				int distance = planetToPlanetDistance[pair<int, int>(currentPlanetID, neighborPlanetID)];

				if(distance < radius){
					
					// need to consider this planet
					heuristic += enemy_distance/distance + enemy_ships*neighbor.NumShips() + 
									enemy_growth_rate*neighbor.GrowthRate();

				}
			}
		}

		//	(v) number of inbound enemy fleets

		for(int j=0;j<enemyFleets.size();++j){

			if(enemyFleets[j].DestinationPlanet() == currentPlanetID)
				heuristic -= fleetScore(enemyFleets[j]);

		}


		//	(vi) number of inbound friendly fleets

		for(int j=0;j<myFleets.size();++j){

			if(myFleets[j].DestinationPlanet() == currentPlanetID)
				heuristic += fleetScore(myFleets[j]);

		}

		myPlanetScore.insert(pair<int, int>(heuristic, currentPlanetID));
	}


	/////////////////////////////////////////////////////////////////////////////////

	out << 3 << endl;

	for(int i=0;i<enemyPlanets.size(); ++i){

		const Planet& currentPlanet = enemyPlanets[i];
	
		int heuristic = 0;

		// assume that the heuristic strength of my planet depends on the following factors:
		//   (i) number of ships presently on the planet

		heuristic += planet_ships*currentPlanet.NumShips();
		
		//   (ii) growth rate

		heuristic += planet_growth_rate*currentPlanet.GrowthRate();


		//   (iii) friendly ships present around the planet
		//   		(a) their distance
		//   		(b) their strength
		//   		(c) their growth rate

		//   		In essence, a function of the following form:
		//   			       a                        -----------
		//   			x = ------- + b*strength + c* \/growth rate  
		//  			    distance                  
		
		for(int j=0;j<enemyPlanets.size();++j){
			
			const Planet& neighbor = enemyPlanets[j];

			if(neighbor.PlanetID() != currentPlanet.PlanetID()){
			
				int distance = planetToPlanetDistance[pair<int, int>(currentPlanet.PlanetID(), neighbor.PlanetID())];
// further change the static parameter distance to a more flexible one which involves the turns
// taken to reach a planet
				if(distance < radius){
					
					// need to consider this planet
					heuristic += friend_distance/distance + friend_ships*neighbor.NumShips() + 
									friend_growth_rate*neighbor.GrowthRate();

				}
			}
		}

		//	(iv) enemy ships present around the planet
		//		a function similar to part (iii) but with different constants
		//		Let's call the value 'y'.
		
		for(int j=0;j<myPlanets.size();++j){
			
			const Planet& neighbor = myPlanets[j];

			if(neighbor.PlanetID() != currentPlanet.PlanetID()){
			
				int distance = planetToPlanetDistance[pair<int, int>(currentPlanet.PlanetID(), neighbor.PlanetID())];

				if(distance < radius){
					
					// need to consider this planet
					heuristic += enemy_distance/distance + enemy_ships*neighbor.NumShips() + 
									enemy_growth_rate*neighbor.GrowthRate();

				}
			}
		}

		//	(v) number of inbound enemy fleets

		for(int j=0;j<myFleets.size();++j){

			if(myFleets[j].DestinationPlanet() == currentPlanet.PlanetID())
				heuristic -= fleetScore(myFleets[j]);

		}


		//	(vi) number of inbound friendly fleets

		for(int j=0;j<enemyFleets.size();++j){

			if(enemyFleets[j].DestinationPlanet() == currentPlanet.PlanetID())
				heuristic += fleetScore(enemyFleets[j]);

		}

		// enemyPlanetScore[currentPlanet.PlanetID()] = heuristic;

		enemyPlanetScore.insert(pair<int, int>(heuristic, currentPlanet.PlanetID()));

		if(heuristic > enemy_score){
			enemy_score = heuristic;
			enemyWeakest = &currentPlanet;
		}

	}

	out<<4<<endl;
	// strength of enemy when all the reinforcements reach it
	if(enemyWeakest == NULL)
		return;

	int strength = enemyWeakest->NumShips();

	for(int i=0;i<enemyFleets.size();++i){

		if(enemyFleets[i].DestinationPlanet() == enemyWeakest->PlanetID()){
			strength += enemyFleets[i].NumShips();
		}

	}

	for(int i=0;i<myFleets.size();++i){

		if(myFleets[i].DestinationPlanet() == enemyWeakest->PlanetID()){
			strength -= myFleets[i].NumShips();
		}
	}

	out<<5<<endl<<endl;
	
	if(strength <= 0)
		return;

	set<pair<int, int>, greater<pair<int, int> > >::iterator it = myPlanetScore.begin();

	bool attacked = false;
	int strength1;
//	out << "My pn: " << myPlanetScore.size();
	for(;!attacked && it != myPlanetScore.end(); ++it){
		pair<int, int> p = *it;
//		out<<"Score: "<<p.first<<endl<<endl;
		Planet myStrongest = pw.GetPlanet(p.second);

		strength1 = strength + enemyWeakest->GrowthRate()*planetToPlanetDistance[pair<int, int>(enemyWeakest->PlanetID(), myStrongest.PlanetID())];
		strength1 *= 1.25;

		if(myStrongest.NumShips() >= strength1){
			pw.IssueOrder(myStrongest.PlanetID(), enemyWeakest->PlanetID(), strength1);
			attacked = true;
		}
		else{
			pw.IssueOrder(myStrongest.PlanetID(), enemyWeakest->PlanetID(), myStrongest.NumShips()/2);
			strength -= myStrongest.NumShips()/2;
		}

	}

	out <<6<<endl<<endl;
	out.flush();
}


void DoTurn(const PlanetWars& pw) {
	if(!calculatedDistances)
		computeDistanceBetweenPlanets(pw);
	
	// Snipe(pw);
	 Attack(pw);
	
	// if(pw.EnemyFleets().size() == 0)
	// 	Attack(pw);
	// else Snipe(pw);
	

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
