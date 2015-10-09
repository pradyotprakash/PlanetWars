#include <iostream>
#include "PlanetWars.h"
#include <fstream>
#include <cmath>
#include <map>
#include <set>
#include "constants.cc"

using namespace std;

typedef map<pair<int, int>, double > mp;
typedef set<pair<int, int>, greater<pair<int, int> > > pii;
typedef map<int, vector<int> > mpIV;
typedef map<int, vector<Fleet> > mpIF;

int turn = 0;

struct Move{
	int source;
	int target;
	int count;
};

std::vector<Move> moves;

mpIV nearestFriends;
mpIV nearestEnemies;
mpIV nearestNeutrals;

mpIF enemyFleets;
mpIF myFleets;
mpIF allFleets;

pii myPlanetScore;
pii enemyPlanetScore;


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


void findNeighbours(int planetID, PlanetWars pw){
	std::vector<Planet> planetList = pw.Planets();
	std::vector<Fleet> fleetList = pw.Fleets();

	std::vector<int> myPlanets;
	std::vector<int> enemyPlanets;
	std::vector<int> neutralPlanets;
	std::vector<Fleet> myFleets1;
	std::vector<Fleet> enemyFleets1;
	std::vector<Fleet> allFleets1;
	
	for(int i=0;i<planetList.size();i++){
		switch(planetList[i].Owner()){
			case 1: //myPlanets
				myPlanets.push_back(planetList[i].PlanetID());
				break;
			case 0: //neutralPlanets
				neutralPlanets.push_back(planetList[i].PlanetID());
				break;
			default:
				enemyPlanets.push_back(planetList[i].PlanetID());
				break;
		};
	}
	
	for(int i=0;i<fleetList.size();i++){
		if(fleetList[i].DestinationPlanet()==planetID){
			switch(fleetList[i].Owner()){
				case 1: //myPlanets
					myFleets1.push_back(fleetList[i]);
					allFleets1.push_back(fleetList[i]);
					break;
				default:
					enemyFleets1.push_back(fleetList[i]);
					allFleets1.push_back(fleetList[i]);					
					break;
			};
		}
	}

	for(int i=0; i<myPlanets.size();i++){                // SORRY FOR THIS :'(    
		for (int j = 0; j < myPlanets.size()-1; ++j)		// #PleaseDontKillMe	
		{
			if(planetToPlanetDistance[pair<int, int>(planetID, myPlanets[j])] > planetToPlanetDistance[pair<int, int>(planetID, myPlanets[j+1])] ){
				int temp = myPlanets[j];
				myPlanets[j] = myPlanets[j+1];
				myPlanets[j+1] = temp;
			}
		}		
	}

	for(int i=0; i<neutralPlanets.size();i++){                // SORRY FOR THIS :'(    
		for (int j = 0; j < neutralPlanets.size()-1; ++j)		// #PleaseDontKillMe	
		{
			if(planetToPlanetDistance[pair<int, int>(planetID, neutralPlanets[j])] > planetToPlanetDistance[pair<int, int>(planetID, neutralPlanets[j+1])] ){
				int temp = neutralPlanets[j];
				neutralPlanets[j] = neutralPlanets[j+1];
				neutralPlanets[j+1] = temp;
			}
		}		
	}

	for(int i=0; i<enemyPlanets.size();i++){                // SORRY FOR THIS :'(    
		for (int j = 0; j < enemyPlanets.size()-1; ++j)		// #PleaseDontKillMe	
		{
			if(planetToPlanetDistance[pair<int, int>(planetID, enemyPlanets[j])] > planetToPlanetDistance[pair<int, int>(planetID, enemyPlanets[j+1])] ){
				int temp = enemyPlanets[j];
				enemyPlanets[j] = enemyPlanets[j+1];
				enemyPlanets[j+1] = temp;
			}
		}		
	}


	for(int i=0; i<myFleets1.size();i++){                // SORRY FOR THIS :'(    
		for (int j = 0; j < myFleets1.size()-1; ++j)		// #PleaseDontKillMe	
		{
			if(myFleets1[j].TurnsRemaining() > myFleets1[j+1].TurnsRemaining() ){
				Fleet temp = myFleets1[j];
				myFleets1[j] = myFleets1[j+1];
				myFleets1[j+1] = temp;
			}
		}		
	}

	for(int i=0; i<enemyFleets1.size();i++){                // SORRY FOR THIS :'(    
		for (int j = 0; j < enemyFleets1.size()-1; ++j)		// #PleaseDontKillMe	
		{
			if(enemyFleets1[j].TurnsRemaining() > enemyFleets1[j+1].TurnsRemaining() ){
				Fleet temp = enemyFleets1[j];
				enemyFleets1[j] = enemyFleets1[j+1];
				enemyFleets1[j+1] = temp;
			}
		}		
	}

	for(int i=0; i<allFleets1.size();i++){                // SORRY FOR THIS :'(    
		for (int j = 0; j < allFleets1.size()-1; ++j)		// #PleaseDontKillMe	
		{
			if(allFleets1[j].TurnsRemaining() > allFleets1[j+1].TurnsRemaining() ){
				Fleet temp = allFleets1[j];
				allFleets1[j] = allFleets1[j+1];
				allFleets1[j+1] = temp;
			}
		}		
	}


	nearestNeutrals[planetID]=neutralPlanets;
	nearestFriends[planetID]=myPlanets;
	nearestEnemies[planetID]=enemyPlanets;
	allFleets[planetID]=allFleets1;
	myFleets[planetID]=myFleets1;
	enemyFleets[planetID]=enemyFleets1; 
}

void snipe(Planet targetPlanet, PlanetWars pw){
	int target = targetPlanet.PlanetID();
	int time = 0;
	int population = targetPlanet.NumShips();
	int owner = 0;

	std::vector<Fleet> fleet = allFleets[target];

	for (int i = 0; i < fleet.size(); ++i)
	{out<<"rem: "<<fleet[i].TurnsRemaining()<<" "<<population<<" is pop ";
	
		int timepass = fleet[i].TurnsRemaining() - time;
		time = fleet[i].TurnsRemaining();

		if (owner != 0){
			population += timepass * targetPlanet.GrowthRate();
		}

		if(fleet[i].Owner() == owner){
			population += fleet[i].NumShips();
  		}

  		else{
  			population -= fleet[i].NumShips();
  			if(population <0){
  				owner = fleet[i].Owner();
  				population *= -1;
  			}
  		}
	}
	if(fleet.size()>0){
		out<<endl<<"population "<<population<<endl;
		out<<"owner "<<owner<<endl;
	}
	if(owner !=2){
		return;
	}
	// else{
	// 	std::vector<int> neighbours = nearestFriends[target];

	// 	for (int i = 0; i < neighbours.size(); ++i)
	// 	{	int distFromNeighbour = ceil(planetToPlanetDistance[pair<int,int>(target,neighbours[i])]);
	// 		if(population+targetPlanet.GrowthRate() +1< pw.GetPlanet(neighbours[i]).NumShips() ){
	// 			if(distFromNeighbour < time){
	// 			//later
	// 			}
	// 			// if( distFromNeighbour == time){
	// 			// 	pw.IssueOrder(neighbours[i],target,popula@tion+targetPlanet.GrowthRate()	+1);
	// 			// 	return;
	// 			// }
	// 			else if(distFromNeighbour > time){
	// 				if(population + (distFromNeighbour - time)*targetPlanet.GrowthRate() < pw.GetPlanet(neighbours[i]).NumShips()){
	// 				pw.IssueOrder(neighbours[i],target,population + (distFromNeighbour - time)*targetPlanet.GrowthRate()	);
	// 				return;
	// 				}
	// 			}
	// 		}
	// 	}
	// }

std::vector<int> neighbours = nearestFriends[target];
out<<"n size "<<neighbours.size();
	for (int i = 0; i < fleet.size(); ++i)
	{	int prevOwner = 0;

		time = fleet[i].TurnsRemaining();
		out<<"time"<<time<<endl;

		if(prevOwner == 2){
			int distFromNeighbour = ceil(planetToPlanetDistance[pair<int,int>(target,neighbours[i])]);
			if(population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate()+1 <  pw.GetPlanet(neighbours[i]).NumShips() ){
				if(distFromNeighbour < time-1){
				out<<"laterDude"<<distFromNeighbour<<endl;
				return;
				}
				else if(distFromNeighbour >= time-1){
					if(population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate() < pw.GetPlanet(neighbours[i]).NumShips()){
					out<<"bheja"<<population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate()+1<<endl;
					out<<"bheja pop"<<population <<endl;
					out<<"bheja owner"<<owner <<endl;
					out<<"bheja time"<<time <<endl;
					out<<"bheja distFromNeighbour"<<distFromNeighbour <<endl;

					pw.IssueOrder(neighbours[i],target,population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate()+1);
					return;
					}
				}
			}

		}

  		else if(owner == 2 && fleet[i+1].Owner()!=1){
		  	for (int i = 0; i < neighbours.size(); ++i)
			{
				int distFromNeighbour = ceil(planetToPlanetDistance[pair<int,int>(target,neighbours[i])]);
				out<<"d="<<distFromNeighbour<<endl;
				if(population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate()+1 <  pw.GetPlanet(neighbours[i]).NumShips() ){
					if(distFromNeighbour <= time){
					out<<"laterDude"<<distFromNeighbour<<endl;
					return;
					}
					else if(distFromNeighbour > time){
						if(population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate() < pw.GetPlanet(neighbours[i]).NumShips()){
						out<<"bheja"<<population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate()+1<<endl;
						out<<"bheja pop"<<population <<endl;
						out<<"bheja owner"<<owner <<endl;
						out<<"bheja time"<<time <<endl;
						out<<"bheja distFromNeighbour"<<distFromNeighbour <<endl;

						pw.IssueOrder(neighbours[i],target,population + (1+distFromNeighbour - time)*targetPlanet.GrowthRate()+1);
						return;
						}
					}
				}
			}

  		}
	}
}

int fleetScore(const Fleet& f){

	return fleet_ships*f.NumShips() - fleet_turns*f.TurnsRemaining();

}

void FindPlanetScores(const PlanetWars& pw){

	myPlanetScore.clear();
	enemyPlanetScore.clear();

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
	}

	out<<4<<endl;

}


void Attack(const PlanetWars& pw){

	vector<Planet> enemyPlanets = pw.EnemyPlanets();
	vector<Fleet> enemyFleets = pw.EnemyFleets();
	vector<Fleet> myFleets = pw.MyFleets();
	vector<Planet> myPlanets = pw.MyPlanets();

	map<int, int> numOfShipsRemaining;

	for(int i=0;i<myPlanets.size();++i){

		numOfShipsRemaining[myPlanets[i].PlanetID()] = myPlanets[i].NumShips();

	}

	// out<<enemyPlanetScore.size()<<" "<<myPlanetScore.size()<<" Sizes\n";

	for(pii::iterator it1 = enemyPlanetScore.begin(); it1 != enemyPlanetScore.end(); ++it1){

		Planet enemy = pw.GetPlanet((*it1).second);
		//out<<"Ships: "<<enemy.NumShips()<<endl<<endl;
		int strength = enemy.NumShips();

		for(int i=0;i<enemyFleets.size();++i){

			if(enemyFleets[i].DestinationPlanet() == enemy.PlanetID()){
				strength += enemyFleets[i].NumShips();
			}

		}

		for(int i=0;i<myFleets.size();++i){

			if(myFleets[i].DestinationPlanet() == enemy.PlanetID()){
				strength -= myFleets[i].NumShips();
			}
		}

		if(strength <= 0)
			continue;

		bool attacked = false;

		for(pii::iterator it2 = myPlanetScore.begin(); !attacked && it2 != myPlanetScore.end(); ++it2){

			if(strength <= 0)
				break;

			Planet me = pw.GetPlanet((*it2).second);

			int strength1 = strength + enemy.GrowthRate()*planetToPlanetDistance[pair<int, int>(enemy.PlanetID(), me.PlanetID())];
			strength1 += 5;

			//out<<"ASA "<<strength<<" "<<strength1<<" "<<me.NumShips()<<endl<<endl;
			
			int p_id = me.PlanetID();
			int p_ships = numOfShipsRemaining[p_id];
			

			if(p_ships >= strength1){
				pw.IssueOrder(me.PlanetID(), enemy.PlanetID(), strength1);
				numOfShipsRemaining[p_id] -= strength1;
				attacked = true;
			}
			else{
				pw.IssueOrder(me.PlanetID(), enemy.PlanetID(), p_ships/2);
				strength -= p_ships/2;
				numOfShipsRemaining[me.PlanetID()] -= p_ships/2;
			}
		}
	}

}

void Defend(const PlanetWars& pw){

	vector<Planet> enemyPlanets = pw.EnemyPlanets();
	vector<Fleet> enemyFleets = pw.EnemyFleets();
	vector<Fleet> myFleets = pw.MyFleets();
	vector<Planet> myPlanets = pw.MyPlanets();

	map<int, int> numOfShipsRemaining;

	for(int i=0;i<myPlanets.size();++i){

		numOfShipsRemaining[myPlanets[i].PlanetID()] = myPlanets[i].NumShips();

	}

	for(int j=0;j<myPlanets.size();++j){

		set<pair<int, int> > distances;

		for(int k=0;k<myPlanets.size();++k){

			if(myPlanets[j].PlanetID() != myPlanets[k].PlanetID()){
				distances.insert(pair<int, int>(planetToPlanetDistance[pair<int, int>(myPlanets[k].PlanetID(), myPlanets[j].PlanetID())], myPlanets[k].PlanetID()));
			}

		}

		for(int i=0;i<enemyFleets.size();++i){

			bool accounted = false;

			if(enemyFleets[i].DestinationPlanet() == myPlanets[j].PlanetID()){

				int xi = float(enemyFleets[i].NumShips() + 10)*float(enemyFleets[i].TurnsRemaining())/float(enemyFleets[i].NumShips() * enemyFleets[i].TotalTripLength());

				// defend this planet from this incoming fleet
				for(set<pair<int, int> >::iterator it = distances.begin();!accounted && it != distances.end();++it){

					int p_id = (*it).second;
					int p_ships = pw.GetPlanet(p_id).NumShips();
					
					if(p_ships >= xi){
						pw.IssueOrder(p_id, myPlanets[j].PlanetID(), xi);
						numOfShipsRemaining[p_id] -= xi;
						accounted = true;
					}
					else{
						pw.IssueOrder(p_id, myPlanets[j].PlanetID(), p_ships/2);
						xi -= p_ships/2;
						numOfShipsRemaining[p_id] -= p_ships/2;
					}
				}

			}

		}
	}
}

void CaptureNeutrals(const PlanetWars& pw){
	
}


void DoTurn(const PlanetWars& pw) {
		
	turn++;
	out<<endl<<"Turn "<<turn<<endl;
	
	if(!calculatedDistances)
		computeDistanceBetweenPlanets(pw);

	FindPlanetScores(pw);
	Attack(pw);
	Defend(pw);

	std::vector<Planet> planetList = pw.Planets();
	for(int i=0;i<planetList.size();i++){
		findNeighbours(planetList[i].PlanetID(),pw);
	}

	
	// std::vector<Planet> neutralList = pw.NeutralPlanets();
	// for(int i=0;i<neutralList.size();i++){
	// 	snipe(neutralList[i],pw);
	// }

	

	// std::vector<Planet> enemyList = pw.EnemyPlanets();
	// for(int i=0;i<enemyList.size();i++){
	// 	attack(enemyList[i]);
	// }

	// std::vector<Planet> FriendlyList = pw.MyPlanets();
	// for(int i=0;i<FriendlyList.size();i++){
	// 	defence(FriendlyList[i]);
	// }
}


// void DoTurn(const PlanetWars& pw) {
// 	if(!calculatedDistances)
// 		computeDistanceBetweenPlanets(pw);
	
// 	 Snipe(pw);
// 	// Attack(pw);
	
// 	// if(pw.EnemyFleets().size() == 0)
// 	// 	Attack(pw);
// 	// else Snipe(pw);
	

// }

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
