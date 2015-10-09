import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
* @author Ashoka Ekanayaka
* ashokae@gmail.com, http://iq-games.blogspot.com
*/
public class MasterBot878 {
    public static void DoTurn(PlanetWars pw) {
		ArrayList<OrderInfo> orders = new ArrayList<OrderInfo>();
        ArrayList <Planet> targetPlanets = new ArrayList<Planet>();
        ArrayList <PlanetDefenceInfo> planetsInfo;

        final List<Planet> myPlanets = pw.MyPlanets();
        List <Planet> enemyPlanets = pw.EnemyPlanets();
        final List <Planet> neutralPlanets = pw.NeutralPlanets();
        final List <Fleet> allFleets = pw.Fleets();
        final List <Fleet> myFleets = pw.MyFleets();
        final List <Fleet> enemyFleets = pw.EnemyFleets();

        final int myPlanetsCount = myPlanets.size();
        final int length = myPlanetsCount - 1;

        //(0) first move
        ArrayList <Planet> myNeutrals = new ArrayList<Planet>();
        ArrayList <Planet> enemyNeutrals = new ArrayList<Planet>();
        ArrayList <Planet> enemyNeutralsAll = new ArrayList<Planet>();

        if ((myPlanetsCount==1) && (enemyPlanets.size()==1)) {
            Planet mPlanet = myPlanets.get(0);
            Planet ePlanet = enemyPlanets.get(0);
            int attackPower1 = mPlanet.NumShips();
            int attackPower = mPlanet.NumShips();
            int distance = pw.Distance(ePlanet, mPlanet);

            // there can be fleets bound for us. find them and make sure we are safe first.
            for (Fleet f : enemyFleets) {
                int id = f.DestinationPlanet();
                if (mPlanet.PlanetID() == id) {
                    attackPower -= f.NumShips();
                    attackPower1 -= f.NumShips();
                } else {
                    enemyNeutrals.add(pw.GetPlanet(id));
                    enemyNeutralsAll.add(pw.GetPlanet(id));
                }
            }

            for (Fleet f : myFleets) {
                if (ePlanet.PlanetID() != f.DestinationPlanet()) {
                    Planet p = pw.GetPlanet(f.DestinationPlanet());
                    if (!myNeutrals.contains(p)) {
                        if (enemyNeutrals.contains(p)) {
                            enemyNeutrals.remove(p);
                        } else {
                            myNeutrals.add(p);
                        }
                    }
                }
            }

            int growth = mPlanet.GrowthRate() * distance;
            int defenceRequirement = getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals, enemyNeutrals,
                    mPlanet, distance);
            int excessImpact = defenceRequirement - growth;
            if (excessImpact > 0) {
                attackPower1 -= excessImpact;
            }

            excessImpact += ePlanet.NumShips();
            if (excessImpact > 0) {
                attackPower -= excessImpact;
            }

            // take the enemy planet if we seem lucky
            int force = getAttackForceSize(ePlanet, myFleets, distance, false, orders);
            force += getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals, enemyNeutrals,
                    ePlanet, distance);
            if ((force >= 0) && (attackPower1 > force)) {
                orders.add(new OrderInfo(mPlanet, ePlanet, force+1));
                targetPlanets.add(ePlanet);
                attackPower = attackPower1 - (force+1);
            }

            // now go after rest of enemy fleets, there can be opportunities to snipe them.
            for (Planet nPlanet : enemyNeutralsAll) {
                if (targetPlanets.contains(nPlanet) || nPlanet.GrowthRate()==0) {
                    continue;       // already taken care of or not interested
                } else { // a neutral Planet which hasn't been processed yet in this turn
                    int strength = nPlanet.NumShips();
                    List <Fleet> orderedFleets = getOrderedFleet(nPlanet, pw.Fleets()); //order the fleets coming this way

                    int conqueror = 0;
                    Fleet f = null;
                    int fleetLength = orderedFleets.size();
                    int lastProcessed = -1;
                    for (int i=0; i < fleetLength; i++) {
                        f = orderedFleets.get(i);
                        if ((i+1) < fleetLength) {
                            Fleet f1 = orderedFleets.get(i+1);
                            if (f1.TurnsRemaining() == f.TurnsRemaining()) {
                                Fleet max1 = f;
                                Fleet max2 = f1;
                                if (f1.NumShips() > f.NumShips()) {
                                    max1 = f1;
                                    max2 = f;
                                }
                                if (max1.NumShips() > strength) {
                                    if (strength > max2.NumShips()) {
                                        strength = max1.NumShips() - strength;
                                    } else {
                                        strength = max1.NumShips() - max2.NumShips();
                                    }
                                    conqueror = max1.Owner();
                                } else {
                                    strength -= max1.NumShips();
                                }
                                if (strength == 0) {
                                    conqueror = 0;
                                }
                                lastProcessed = i+1;
                                if (conqueror == 0) {
                                    i++; // we have processed two elements, skip the next
                                    continue;
                                } else {
                                    break;
                                }
                            }
                        }

                        strength -= f.NumShips();
                        lastProcessed = i;
                        if (strength < 0) {
                            conqueror = f.Owner();
                            break;
                        }
                    }

                    for (int i=0; i<=lastProcessed; i++) {
                        orderedFleets.remove(0);  // remove items from top till lastProcessed
                    }

                    if (conqueror == 0) {
                        continue; // no chance for snipe yet, todo we could consider this planet under the normal mode
                    }

                    nPlanet.Owner(conqueror);
                    orderedFleets = getOrderedFleet(nPlanet, orderedFleets);

                    int mDistance = pw.Distance(mPlanet.PlanetID(), nPlanet.PlanetID());
                    int eDistance = pw.Distance(ePlanet.PlanetID(), nPlanet.PlanetID());

                    if ((mDistance <= f.TurnsRemaining()) || ((conqueror==1) && orderedFleets.isEmpty())) {
                        continue; // need to wait until we can snipe
                    }

                    int enemyImpact = getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals,
                            enemyNeutrals, nPlanet, mDistance);

                    if (mDistance > eDistance) {
                        enemyImpact += (ePlanet.NumShips() + ePlanet.GrowthRate() * (mDistance-eDistance));
                    }

                    // we are treating as if this planet is already occupied so deduct the additional growth
                    strength = Math.abs(strength);
                    strength -= f.TurnsRemaining() * nPlanet.GrowthRate();
                    nPlanet.NumShips(strength);
                    strength = getAttackForceSize(nPlanet, orderedFleets, mDistance, true, orders) + enemyImpact;
                    if (strength >= 0) {  // todo need a measurement to compare the benefit of this against the cost
                        if (attackPower > strength) {
                            orders.add(new OrderInfo(mPlanet, nPlanet, strength+1));
                            targetPlanets.add(nPlanet);
                            attackPower -= (strength+1);
                        }
                    }
                }
            }

            // select the neutral planets which are ok for taking and sort them descending on score (growth per time per ship)
            int defenceReserve = ePlanet.NumShips() - growth;
            ArrayList <Planet> neutrals = new ArrayList<Planet>();
            for (Planet nPlanet : neutralPlanets) {
                boolean already = false;
                for (Fleet fleet : pw.Fleets()) {
                    if (nPlanet.PlanetID() == fleet.DestinationPlanet()) {
                        already = true;
                        break;
                    }
                }

                // if we have enemy fleets this wud have been handled in the earlier case.
                // if we don't have enemy fleets (then no point sending 2 fleets there)
                if (already || (nPlanet.GrowthRate() == 0)) {
                    continue;
                }

                int mDistance = pw.Distance(mPlanet, nPlanet);
                int eDistance = pw.Distance(ePlanet, nPlanet);

                // if this planet is close enough, it can send us reinforcements b4 a direct enemy attack
                // this will help us to reduce the reserve needed for guarding against such an attack. This bonus can not exceed the reserve amount
                int bonus = getBonus(pw, mPlanet, distance, defenceReserve, nPlanet);

                if (isFirstMove(pw, mPlanet, ePlanet)) {
                    if (((mDistance < eDistance) || ((mDistance == eDistance) && (nPlanet.NumShips() <= nPlanet.GrowthRate())))
                            && (attackPower + bonus > nPlanet.NumShips())) {
                        neutrals = addToList(pw, neutrals, nPlanet, mPlanet, ePlanet);
                    }
                } else {
                    int longerDistance = mDistance;
                    if (eDistance > mDistance) {
                        longerDistance = eDistance;
                    }
                    int enemyImpact = getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals, enemyNeutrals, nPlanet, longerDistance);
                    enemyImpact += nPlanet.NumShips() + ePlanet.NumShips();
                    enemyImpact -= mPlanet.NumShips();
                    enemyImpact += ePlanet.GrowthRate() * (mDistance-eDistance);  //whoever is closer will get an advantage
                    enemyImpact += nPlanet.GrowthRate() * (mDistance-eDistance);  //whoever is closer will get an advantage

                    if ((attackPower + bonus > nPlanet.NumShips()) && (enemyImpact <= 0)) {
                        neutrals = addToList(pw, neutrals, nPlanet, mPlanet, ePlanet);
                    }
                }
            }

            // now that we have a pool, pick best ones and attack.
            if (neutrals.size() > 0) {
                for (Planet p1 : neutrals) {
                    int nStrength = p1.NumShips();
                    int bonus = getBonus(pw, mPlanet, distance, defenceReserve, p1);
                    if (attackPower + bonus > nStrength) {
                        int forceSize = nStrength + 1;
                        defenceReserve -= bonus;
                        attackPower += bonus;
                        orders.add(new OrderInfo(mPlanet, p1, forceSize));
                        targetPlanets.add(p1);
                        attackPower -= forceSize;

                        if (attackPower <= 0) {
                            break;
                        }
                    }
                }
            }

            // execute all orders
            for (OrderInfo order : orders) {
                if ((order.source.NumShips() >= order.force) && (order.source.Owner() == 1) && (order.force>0)) {
                    pw.IssueOrder(order.source, order.target, order.force);
                }
            }

            return;
        }

        if (myPlanetsCount < 1) {
            return;
        }

        int enemyCount = enemyPlanets.size();
        Hashtable <Integer, ArrayList<Fleet>> planetFleets =
                new Hashtable<Integer, ArrayList<Fleet>>(enemyCount);
        for (Planet p : enemyPlanets) {
            planetFleets.put(p.PlanetID(), new ArrayList<Fleet>());
        }

        for (Fleet f : allFleets) {
            Planet planet = pw.GetPlanet(f.DestinationPlanet());
            if (planet.Owner() == 0) {
                if (f.Owner() == 1) {
                    if (!myNeutrals.contains(planet)) {
                        if (enemyNeutrals.contains(planet)) {
                            enemyNeutrals.remove(planet);
                        } else {
                            myNeutrals.add(planet);
                        }
                    }
                }

                if (f.Owner() == 2) {
                    if (!enemyNeutrals.contains(planet)) {
                        if (myNeutrals.contains(planet)) {
                            myNeutrals.remove(planet);
                        } else {
                            enemyNeutrals.add(planet);
                        }
                    }
                }
            } else if (planet.Owner() == 2) {
                planetFleets.put(planet.PlanetID(), addToList(planetFleets.get(planet.PlanetID()), f));
            }
        }

        planetsInfo = checkAndOrderPlanets(myPlanets, allFleets, orders);
        enemyPlanets = getSorted(pw, enemyPlanets, planetsInfo.get(length).planet, 1);

        if (pw.Production(1) > pw.Production(2)) {
            defend(pw, orders, targetPlanets, planetsInfo, allFleets, myFleets, enemyFleets, myPlanetsCount, length, myNeutrals, enemyNeutrals);
            snipe(pw, orders, targetPlanets, planetsInfo, enemyPlanets, neutralPlanets, myFleets, enemyFleets, length, myNeutrals, enemyNeutrals);
            attack(pw, orders, targetPlanets, planetsInfo, myPlanets, enemyPlanets, allFleets, myFleets, enemyFleets, myPlanetsCount, myNeutrals, enemyNeutrals, planetFleets);
        } else {
            attack(pw, orders, targetPlanets, planetsInfo, myPlanets, enemyPlanets, allFleets, myFleets, enemyFleets, myPlanetsCount, myNeutrals, enemyNeutrals, planetFleets);
            defend(pw, orders, targetPlanets, planetsInfo, allFleets, myFleets, enemyFleets, myPlanetsCount, length, myNeutrals, enemyNeutrals);
            snipe(pw, orders, targetPlanets, planetsInfo, enemyPlanets, neutralPlanets, myFleets, enemyFleets, length, myNeutrals, enemyNeutrals);
        }

        // (3) check on neutral planets for expanding
        PlanetDefenceInfo s2 = null;
        Planet d2 = null;
        int avgShipsPerGrowth = pw.NumShips(1) / pw.Production(1);
        int nearestEnemyDistance = 0;

        for (Planet neutralPlanet : neutralPlanets) {
            if (neutralPlanet.GrowthRate() == 0) {
                continue;
            }
            boolean already = false;
            for (Fleet f : myFleets) {        // enemy fleets were handled in earlier case.
                if (neutralPlanet.PlanetID() == f.DestinationPlanet()) { // handled under sniper
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }

            int force = neutralPlanet.NumShips();
            int avg = (force / neutralPlanet.GrowthRate());
            if ((avg - avgShipsPerGrowth) > 10) {
                continue;
            }

            int enemyDistance = getClosestEnemyDistance(pw, neutralPlanet);
            PlanetDefenceInfo friendlyPlanet = getClosestFriendlyPlanet(pw, planetsInfo, neutralPlanet, orders);
            if (friendlyPlanet == null) {
                continue;
            }
            int friendlyDistance = pw.Distance(neutralPlanet, friendlyPlanet.planet);
            int distanceGap = enemyDistance - friendlyDistance;
            if (distanceGap <= 0) {
                continue;
            }

            int score = avg - distanceGap;
            if (score < avgShipsPerGrowth) {
                nearestEnemyDistance = enemyDistance;
                avgShipsPerGrowth = score;
                d2 = neutralPlanet;
                s2 = friendlyPlanet;
            }
        }

        if (s2 != null) {
            int force = d2.NumShips() + 1;
            int myLowestStrength = getSafeAttackingStrength(pw, planetsInfo, s2, orders);
            Planet e2 = getClosestEnemyPlanet(pw, s2.planet);
            if (e2 != null) {
                int bonus = getBonus(pw, s2.planet, pw.Distance(s2.planet, e2), e2.NumShips(), d2);
                myLowestStrength += bonus;
                if (myLowestStrength > s2.strength) {
                    myLowestStrength = s2.strength;
                }
            }
            
            int remainder = force - myLowestStrength;
            if (myLowestStrength >= force) {
                orders.add(new OrderInfo(s2.planet,d2,force));
                targetPlanets.add(d2);
                int index = planetsInfo.indexOf(s2);
                s2.strength -= force;
                planetsInfo.set(index, s2);
            } else {  // find another planet to finish off the remainder
                int total = 0;
                ArrayList<DefenceForceInfo> sources = new ArrayList<DefenceForceInfo>();
                for (int i=length; i>=0; i--) {
                    PlanetDefenceInfo info = planetsInfo.get(i);
                    Planet myPlanet = info.planet;
                    if (myPlanet.PlanetID() == s2.planet.PlanetID()) {
                        continue;
                    }

                    int distance = pw.Distance(myPlanet, d2);
                    if (distance > nearestEnemyDistance) { // too risky?
                        continue;
                    }

                    force = getSafeAttackingStrength(pw, planetsInfo, info, orders);
                    if (force > 0) {
                        total += force;
                        addToList(sources, new DefenceForceInfo(i, info, distance, remainder, force));
                    }
                }

                if (total >= remainder) {
                    targetPlanets.add(d2);
                    orders.add(new OrderInfo(s2.planet,d2,myLowestStrength));
                    int tmpIndex = planetsInfo.indexOf(s2);
                    s2.strength -= myLowestStrength;
                    planetsInfo.set(tmpIndex, s2);

                    for (DefenceForceInfo info : sources) {
                        int contribution = info.available;
                        if (remainder < contribution) {
                            contribution = remainder;
                        }
                        remainder -= contribution;
                        PlanetDefenceInfo planetInfo = info.planetInfo;
                        orders.add(new OrderInfo(planetInfo.planet, d2, contribution));
                        planetInfo.strength -= contribution;
                        planetsInfo.set(info.index, planetInfo);
                        if (remainder <= 0 ) {
                            break;
                        }
                    }
                }
            }
        }

        //(5) move ships from further and safer planets into ones near the front
        if ((myPlanetsCount > 1) && (enemyPlanets.size() > 0)) {
            ArrayList <DefenceForceInfo> list = new ArrayList<DefenceForceInfo>();
            for (int i=0; i<myPlanetsCount; i++) {
                PlanetDefenceInfo info = planetsInfo.get(i);
                int shortestThisRnd = getClosestEnemyDistance(pw, info.planet);
                int force = getSafeAttackingStrength(pw, planetsInfo, info, orders);
                list = addToList(list, new DefenceForceInfo(i, info, shortestThisRnd, force, info.strength));
            }

            int limit = list.size()-1;
            for (int i=0; i<limit; i++) {
                int distance = 100;
                DefenceForceInfo target = list.get(i);
                Planet tp = target.planetInfo.planet;
                DefenceForceInfo source = null;
                int tmpIndex = 0;
                int force = 0;
                for (int j=limit; j>i; j--) {
                    DefenceForceInfo tmpSource = list.get(j);
                    Planet sp = tmpSource.planetInfo.planet;
                    int attackForce = tmpSource.force;
                    Planet ep2 = getClosestEnemyPlanet(pw,sp);
                    Planet ep1 = getClosestEnemyPlanet(pw,tp);
                    int distanceToEnemy = pw.Distance(sp,ep2);
                    int myClosest = pw.Distance(sp, tp);
                    if ((ep1 != null) && (ep2 != null) && (ep1.PlanetID() == ep2.PlanetID())
                        && (myClosest < distanceToEnemy)) {
                        attackForce = tmpSource.available;
                    }

                    if ((myClosest < distance) && (target.distance < tmpSource.distance) &&
                            (attackForce > 0) && (myClosest < distanceToEnemy)) {
                        distance = myClosest;
                        source = tmpSource;
                        tmpIndex = j;
                        force = attackForce;
                    }
                }

                if (source != null) {
                    orders.add(new OrderInfo(source.planetInfo.planet, tp, force));
                    source.planetInfo.strength -= force;
                    planetsInfo.set(source.index, source.planetInfo);
                    source.available -= force;
                    source.force = 0;
                    list.set(tmpIndex, source);
                }

                if (tmpIndex != i+1) {   //this planet is going to loose a receiver. find one
                    source = list.get(i+1);
                    if ((source.force == 0) || (source.available == 0)) {
                        continue;
                    }
                    Planet sp = source.planetInfo.planet;
                    int attackForce = source.force;
                    Planet ep2 = getClosestEnemyPlanet(pw,sp);
                    int distanceToEnemy = pw.Distance(sp,ep2);
                    distance = 100;
                    target = null;
                    for (int j=i; j>=0; j--) {      // find the closest target
                        tp = list.get(j).planetInfo.planet;
                        Planet ep1 = getClosestEnemyPlanet(pw,tp);
                        int myClosest = pw.Distance(sp, tp);
                        if ((ep1 != null) && (ep2 != null) && (ep1.PlanetID() == ep2.PlanetID())
                                && (myClosest < distanceToEnemy)) {
                            attackForce = source.available;
                        }

                        if ((myClosest < distance) && (list.get(j).distance < source.distance) &&
                            (attackForce > 0) && (myClosest < distanceToEnemy)) {
                            distance = myClosest;
                            force = attackForce;
                            target = list.get(j);
                        }
                    }

                    if (target != null) {
                        orders.add(new OrderInfo(sp, target.planetInfo.planet, force));
                        source.planetInfo.strength -= force;
                        planetsInfo.set(source.index, source.planetInfo);
                        source.available -= force;
                        source.force = 0;
                        list.set(tmpIndex, source);
                    }
                }
            }
        }

        // execute all orders
		for (OrderInfo order : orders) {
			if ((order.source.NumShips() >= order.force) && (order.source.Owner() == 1) && (order.force>0)) {
				pw.IssueOrder(order.source, order.target, order.force);
			}
		}
    }

    private static int getBonus(PlanetWars pw, Planet mPlanet, int distance, int defenceReserve, Planet p1) {
        int bonus = 0;
        if (defenceReserve > 0) {
            int returnTripDistanceGap = distance - pw.Distance(mPlanet, p1) *2;
            if (returnTripDistanceGap > 0) {
                bonus = returnTripDistanceGap * p1.GrowthRate();
            }
            if (bonus > defenceReserve) {
                bonus = defenceReserve;
            }
        }
        return bonus;
    }

    private static boolean isFirstMove(PlanetWars pw, Planet mPlanet, Planet ePlanet) {  //method get called only from initial state : we know there is only 1 planet each for us and for enemy
        return pw.Fleets().isEmpty() && (mPlanet.NumShips()==100) && (ePlanet.NumShips()==100);
    }

    // (2) find opportunities to snipe. to arrive at a neutral planet just after the enemy has done the hard work.
    private static void snipe(PlanetWars pw, ArrayList<OrderInfo> orders, ArrayList<Planet> targetPlanets, ArrayList<PlanetDefenceInfo> planetsInfo, List<Planet> enemyPlanets, List<Planet> neutralPlanets, List<Fleet> myFleets, List<Fleet> enemyFleets, int length, ArrayList<Planet> myNeutrals, ArrayList<Planet> enemyNeutrals) {
        for (Fleet ef : enemyFleets) {
            Planet neutral = pw.GetPlanet(ef.DestinationPlanet());
            if ((neutral.Owner() != 0) ||(targetPlanets.contains(neutral))) { // if this is not a neutral go to next
                continue;
            }

            neutralPlanets.remove(neutral); // to help the next module

            ArrayList <Fleet> orderedFleets = getOrderedFleet(neutral, pw.Fleets()); //order the fleets coming this way

            //  check until someone capture this planet. Since we always send a fleet capable of completely toppling a neutral,
            //  our fleet landing would not occur b4 this event. if it occurs, it is always a one which topples the neutral
            int enemyStrength = neutral.NumShips();
            int conqueror = 0;
            int fleetLength = orderedFleets.size();
            int lastProcessed = -1;
            Fleet f = null;
            for (int i=0; i < fleetLength; i++) {
                f = orderedFleets.get(i);
                if ((i+1) < fleetLength) {
                    Fleet f1 = orderedFleets.get(i+1);
                    if (f1.TurnsRemaining() == f.TurnsRemaining()) {
                        Fleet max1 = f;
                        Fleet max2 = f1;
                        if (f1.NumShips() > f.NumShips()) {
                            max1 = f1;
                            max2 = f;
                        }
                        if (max1.NumShips() > enemyStrength) {
                            if (enemyStrength > max2.NumShips()) {
                                enemyStrength = max1.NumShips() - enemyStrength;
                            } else {
                                enemyStrength = max1.NumShips() - max2.NumShips();
                            }
                            conqueror = max1.Owner();
                        } else {
                            enemyStrength -= max1.NumShips();
                        }
                        if (enemyStrength == 0) {
                            conqueror = 0;
                        }
                        lastProcessed = i+1;
                        if (conqueror == 0) {
                            i++; // we have processed two elements, skip the next
                            continue;
                        } else {
                            break;
                        }
                    }
                }

                enemyStrength -= f.NumShips();
                lastProcessed = i;
                if (enemyStrength < 0) {
                    conqueror = f.Owner();
                    break;
                }
            }

            for (int i=0; i<=lastProcessed; i++) {
                orderedFleets.remove(0);  // remove items from top till lastProcessed
            }

            if (conqueror == 0) {
                continue; // no chance for snipe yet, todo we could consider this planet under the normal mode
            }

            neutral.Owner(conqueror);
            orderedFleets = getOrderedFleet(neutral, orderedFleets);

            if ((conqueror==1) && orderedFleets.isEmpty()) {
				continue;
			}

            // we are treating as if this planet is already occupied so deduct the additional growth
            enemyStrength = Math.abs(enemyStrength);
            int touchDown = f.TurnsRemaining();
            enemyStrength -= touchDown * neutral.GrowthRate();

            PlanetDefenceInfo sniper = null;
            int sniperIndex = -1;
            Planet sniped = null;
            int force = 0;
            neutral.NumShips(enemyStrength);
            int score = 100;

            for (int i=length; i>=0; i--) {       // take each of our planets and find the best sniper
                PlanetDefenceInfo info = planetsInfo.get(i);
                int myLowestStrength = getSafeAttackingStrength(pw, planetsInfo, info, orders);
                if (myLowestStrength <= 0) {
                    continue; // we are under severe attack, stay put and wait for reinforcements
                }
                int distance = pw.Distance(info.planet, neutral);
                int reachGap = distance - touchDown;  // amount of time the enemy holding the planet b4 the sniper
                if (reachGap <= 0) {
                    continue; // need to wait until we can snipe
                }

                enemyStrength = getAttackForceSize(neutral, orderedFleets, distance, true, orders);
                for (Planet p : enemyPlanets) {
                    int enemyDistance = pw.Distance(p, neutral);
                    if (enemyDistance <= distance) {
                        enemyStrength += p.NumShips() + p.GrowthRate()*(distance-enemyDistance);
                    }
                }

                for (PlanetDefenceInfo myInfo : planetsInfo) {
                    Planet mp = myInfo.planet;
                    if (info.planet.PlanetID() == mp.PlanetID()) {
                        continue;
                    }
                    int myDistance = pw.Distance(mp, neutral);
                    if (myDistance <= distance) {
                        int strength = getSafeAttackingStrength(pw, planetsInfo, myInfo, orders);
                        if (strength > 0) {
                            enemyStrength -= strength; // + mp.GrowthRate()*(distance-myDistance);
                        }
                    }
                }

                enemyStrength += getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals, enemyNeutrals,
                        neutral, distance);

                if ((enemyStrength>=0) && (myLowestStrength > enemyStrength) && (distance < score)) {
                    score = distance;
                    sniper = info;
                    sniperIndex = i;
                    force = enemyStrength + 1;
                    sniped = neutral;
                }
            }

            if (sniper != null) {
                orders.add(new OrderInfo(sniper.planet, sniped, force));
                targetPlanets.add(sniped);
                sniper.strength -= force;
                planetsInfo.set(sniperIndex, sniper);
            }
        }
    }

    // (1) defend planets under attack
    private static void defend(PlanetWars pw, ArrayList<OrderInfo> orders, ArrayList<Planet> targetPlanets, ArrayList<PlanetDefenceInfo> planetsInfo, List<Fleet> allFleets, List<Fleet> myFleets, List<Fleet> enemyFleets, int myPlanetsCount, int length, ArrayList<Planet> myNeutrals, ArrayList<Planet> enemyNeutrals) {
        for (int i = 0; i < myPlanetsCount; i++) {
            PlanetDefenceInfo targetInfo = planetsInfo.get(i);
            if (targetInfo.strength >= 0) {
                continue;         // this one doesn't need to be defended
            }
            Planet target = targetInfo.planet;
            ArrayList<DefenceForceInfo> sources1 = new ArrayList<DefenceForceInfo>();
            ArrayList<Fleet> fleets = getOrderedFleet(target, allFleets);
            for (int j = length; j >= 0; j--) {
                PlanetDefenceInfo sourceInfo = planetsInfo.get(j);
                Planet src = sourceInfo.planet;
                if ((src.PlanetID() == target.PlanetID())) {
                    break;   // no point searching beyond here, among weaker planets
                }
                int strength1 = sourceInfo.strength;
                if (strength1 <= 0) {
                    continue;
                }
                int distance = pw.Distance(target, src);
                int force = getAttackForceSize(target, fleets, distance, true, orders);
                force += getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals, enemyNeutrals,
                        target, distance);
                if (force >= 0) {
                    sources1 = addToList(sources1, new DefenceForceInfo(j, sourceInfo, distance, force + 1, strength1));
                }
            }

            if (!sources1.isEmpty()) {
                int mustardStrength = 0;
                int limit = -1;
                int lastAmount = 0;
                int forcesLength = sources1.size();
                for (int k = 0; k < forcesLength; k++) {
                    DefenceForceInfo info = sources1.get(k);
                    mustardStrength += info.available;
                    if (info.force <= mustardStrength) {
                        targetPlanets.add(target);
                        limit = k;
                        lastAmount = info.available - (mustardStrength - info.force);
                        break;
                    }
                }

                for (int k = 0; k <= limit - 1; k++) {
                    DefenceForceInfo info = sources1.get(k);
                    orders.add(new OrderInfo(info.planetInfo.planet, target, info.available));
                    info.planetInfo.strength -= info.available;
                    planetsInfo.set(info.index, info.planetInfo);
                }

                if ((limit > -1) && (lastAmount > 0)) {
                    DefenceForceInfo info = sources1.get(limit);
                    orders.add(new OrderInfo(info.planetInfo.planet, target, lastAmount));
                    info.planetInfo.strength -= lastAmount;
                    planetsInfo.set(info.index, info.planetInfo);
                }
            }
        }
    }

    private static void attack(PlanetWars pw, ArrayList<OrderInfo> orders, ArrayList<Planet> targetPlanets,
            ArrayList<PlanetDefenceInfo> planetsInfo, List<Planet> myPlanets, List<Planet> enemyPlanets,
            List<Fleet> allFleets, List<Fleet> myFleets, List<Fleet> enemyFleets, int myPlanetsCount,
            ArrayList<Planet> myNeutrals, ArrayList<Planet> enemyNeutrals, Hashtable <Integer, ArrayList<Fleet>> planetFleets) {
        int max = pw.NumShips(1)+pw.NumShips(2); //an upper bound value to use when sorting enemy planets so that they get infront our planets when distance is same.
        for (Planet enemyPlanet : enemyPlanets) {
            if (targetPlanets.contains(enemyPlanet)) {
                continue;
            }

            ArrayList<Fleet> fleets = getOrderedFleet(enemyPlanet, allFleets);
            ArrayList<DefenceForceInfo> candidates = new ArrayList<DefenceForceInfo>();
            Planet nearestAttacker = getClosestPlanet(pw, enemyPlanet, myPlanets);

            for (int i=0; i<myPlanetsCount; i++) {
                PlanetDefenceInfo info = planetsInfo.get(i);
                int distance = pw.Distance(info.planet, enemyPlanet);
                int strength = getSafeAttackingStrength(pw,planetsInfo,info,orders);
                if ((nearestAttacker != null) && (enemyPlanet.GrowthRate() >= info.planet.GrowthRate())
                        && (nearestAttacker.PlanetID() == info.planet.PlanetID())) {
                    strength = info.strength;
                }
                if (strength > 0) {
                    candidates = addToList(candidates, new DefenceForceInfo(i, info, distance, strength, info.strength));
                }
            }

            for (Planet p : enemyPlanets) {
                if (p.PlanetID() == enemyPlanet.PlanetID()) {
                    continue;
                }
                int distance = pw.Distance(enemyPlanet, p);
                candidates = addToList(candidates, new DefenceForceInfo(0, new PlanetDefenceInfo(0,0,p),
                        distance, p.NumShips(), max));
            }

            int limit = candidates.size();
            int enemyStrength = 0;
            int myStrength = 0;
            int runningDistance = 0;
            int enemyCurrentStrength = 0;
            int j;
            for (j=0; j<limit; j++) {
                DefenceForceInfo candidate = candidates.get(j);
                PlanetDefenceInfo info = candidate.planetInfo;
                Planet p = info.planet;
                if (p.Owner() == 2) {
                    enemyStrength += p.NumShips();
                }else {
                    myStrength += candidate.force;
                }

                int thisDistanceIncrement = candidate.distance-runningDistance;
                int fleetStrength = 0;
                for (int k=0; k<j; k++) {
                    DefenceForceInfo enemyInfo = candidates.get(k);
                    Planet enemySupporter = enemyInfo.planetInfo.planet;
                    if (enemySupporter.Owner() == 2) {
                        enemyStrength += thisDistanceIncrement * enemySupporter.GrowthRate();
                        for (Fleet f : planetFleets.get(enemySupporter.PlanetID())) {
                            if (f.TurnsRemaining() + enemyInfo.distance <= candidate.distance) {
                                if (f.Owner() == 2) {
                                    fleetStrength += f.NumShips();
                                } else {
                                    fleetStrength -= f.NumShips();
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }

                enemyCurrentStrength = getAttackForceSize(enemyPlanet, fleets, candidate.distance, true, orders);
                enemyCurrentStrength += getFuturePlanetImpact(pw, myFleets, enemyFleets, myNeutrals, enemyNeutrals,
                        enemyPlanet, candidate.distance);
                enemyCurrentStrength += fleetStrength;

                if (enemyStrength + enemyCurrentStrength < myStrength) {
                    break;    // this enemy planet probably can be broken (except for fleets heading to it's supporters)
                }

                runningDistance = candidate.distance;
            }

            int total = enemyStrength + enemyCurrentStrength;
            if ((j<limit) && (total >= 0)) { // attack
                targetPlanets.add(enemyPlanet);
                for (int k=0; k<=j; k++) {
                    DefenceForceInfo dInfo = candidates.get(k);
                    if (dInfo.planetInfo.planet.Owner() == 1) {
                        if (dInfo.force > total) {
                            dInfo.force = total + 1;
                        }
                        orders.add(new OrderInfo(dInfo.planetInfo.planet, enemyPlanet, dInfo.force));
                        dInfo.planetInfo.strength -= dInfo.force;
                        planetsInfo.set(dInfo.index, dInfo.planetInfo);
                        total -= dInfo.force;
                    }
                }
            }
        }
    }

    private static int getFuturePlanetImpact(PlanetWars pw, List<Fleet> myFleets, List<Fleet> enemyFleets,
            ArrayList<Planet> myNeutrals, ArrayList<Planet> enemyNeutrals, Planet nPlanet, int mDistance) {
        int enemyImpact = 0;
        enemyImpact += getImpactOfOneSide(pw, enemyFleets, enemyNeutrals, nPlanet, mDistance);
        enemyImpact -= getImpactOfOneSide(pw, myFleets, myNeutrals, nPlanet, mDistance);

        return enemyImpact;
    }

    private static int getImpactOfOneSide(PlanetWars pw, List<Fleet> fleets, ArrayList<Planet> neutrals, Planet nPlanet, int mDistance) {
        int enemyImpact = 0;
        for (Planet p : neutrals) {
            if (p.PlanetID() == nPlanet.PlanetID()) {
                continue;
            }

            ArrayList <Fleet>boundedFleets = getOrderedFleet(p, fleets);
            if (boundedFleets.size() == 0) {
                continue;
            }

            int distance = pw.Distance(p, nPlanet);
            if (distance >= mDistance) {
                continue;
            }

            int turnsToOwn = 100;
            int runningStrength = -p.NumShips();
            for (Fleet fleet : boundedFleets) {
                runningStrength += fleet.NumShips();
                if (runningStrength > 0) {
                    turnsToOwn = fleet.TurnsRemaining();
                    break;
                }
            }

            int excess = mDistance - (distance+turnsToOwn);
            if (excess >= 0) {
                enemyImpact += (runningStrength + p.GrowthRate()*excess);
            }
        }

        return enemyImpact;
    }

    private static int getSafeAttackingStrength(PlanetWars pw, ArrayList<PlanetDefenceInfo> planetsInfo, PlanetDefenceInfo info, List<OrderInfo> orders) {
        int myLowestStrength = info.strength;
        if (myLowestStrength <= 0) {
            return myLowestStrength;
        }

        Planet myPlanet = info.planet;
        Planet ep = getClosestEnemyPlanet(pw, myPlanet);
        if (ep != null) {
            int distance = pw.Distance(ep, myPlanet);
            int enemyAttackForce = ep.NumShips();
            PlanetDefenceInfo fp = getClosestFriendlyPlanet(pw, planetsInfo, myPlanet);
            if (fp != null) {
                int fDistance = pw.Distance(fp.planet, myPlanet);
                if (fDistance <= distance) {
                    enemyAttackForce -= fp.strength; // if the friend is weak and strength is minus, that will add to our worries.
                }
                if (enemyAttackForce <= 0) {
                    return myLowestStrength;
                }
            }

            int excess = Math.abs(getAttackForceSize(myPlanet, pw.Fleets(), distance, false, orders)) - enemyAttackForce;

            if (excess < myLowestStrength) {    // return which ever is the lowest
                myLowestStrength = excess;
            }
        }

        return myLowestStrength;
    }

    private static int getClosestEnemyDistance(PlanetWars pw, Planet p) {
        List <Planet> planets = pw.EnemyPlanets();
        return getClosestDistance(pw, p, planets);
    }

    private static int getClosestDistance(PlanetWars pw, Planet planet, List<Planet> planets) {
        int shortest = 100;
        for (Planet p : planets) {
            if (p.PlanetID() == planet.PlanetID()) {
                continue;
            }
            int distance = pw.Distance(p, planet);
            if (distance < shortest) {
                shortest = distance;
            }
        }
        return shortest;
    }

    private static PlanetDefenceInfo getClosestFriendlyPlanet(PlanetWars pw, ArrayList<PlanetDefenceInfo> planetsInfo, Planet p, List<OrderInfo> orders) {
        int shortest = 100;
        PlanetDefenceInfo nearest = null;
        ArrayList <PlanetDefenceInfo> planets = new ArrayList<PlanetDefenceInfo>(planetsInfo.size()); // copy to not harm the global
        for (PlanetDefenceInfo info : planetsInfo) {
            if (info.planet.PlanetID() == p.PlanetID()) {   // so that the same 2 planets don't refer to each other
                continue;
            }
            planets.add(info);
        }
        for (PlanetDefenceInfo fp : planets) {
            if (getSafeAttackingStrength(pw, planets, fp, orders) <= 0) {   // return only a friend who is in a position to help
                continue;
            }
            int distance = pw.Distance(p, fp.planet);
            if (distance < shortest) {
                shortest = distance;
                nearest = fp;
            }
        }
        return nearest;
    }

    private static Planet getClosestEnemyPlanet(PlanetWars pw, Planet p) {
        List <Planet> planets = pw.EnemyPlanets();
        return getClosestPlanet(pw, p, planets);
    }

    private static PlanetDefenceInfo getClosestFriendlyPlanet(PlanetWars pw, List<PlanetDefenceInfo> planetsInfo, Planet p) {
        int shortest = 100;
        PlanetDefenceInfo nearest = null;
        for (PlanetDefenceInfo fp : planetsInfo) {
            if (fp.planet.PlanetID() == p.PlanetID()) {
                continue;
            }
            if (fp.strength <= 0) {   // return only a friend who is in a position to help
                continue;
            }
            int distance = pw.Distance(p, fp.planet);
            if (distance < shortest) {
                shortest = distance;
                nearest = fp;
            }
        }
        return nearest;
    }

    private static int getClosestPlanetDistance(PlanetWars pw, Planet planet, List<Planet> planets, int limit) {
        for (Planet p : planets) {
            if (p.PlanetID() == planet.PlanetID()) {
                continue;
            }
            int distance = pw.Distance(p, planet);
            if (distance < limit) {
                limit = distance;
            }
        }
        return limit;
    }

    private static Planet getClosestPlanet(PlanetWars pw, Planet planet, List<Planet> planets) {
        int shortest = 100;
        Planet nearest = null;
        for (Planet p : planets) {
            if (p.PlanetID() == planet.PlanetID()) {
                continue;
            }
            int distance = pw.Distance(p, planet);
            if (distance < shortest) {
                shortest = distance;
                nearest = p;
            }
        }
        return nearest;
    }

    private static int getCurrentStrength(Planet source1,List<OrderInfo> orders) {
        int currentStrength = source1.NumShips();
        for (OrderInfo order : orders) {
            if (order.source.PlanetID() == source1.PlanetID()) {
                currentStrength -= order.force;
            }
        }
        return currentStrength;
    }

    public static ArrayList<PlanetDefenceInfo> checkAndOrderPlanets(List <Planet> myPlanets, List <Fleet> fleet, List <OrderInfo> orders) {
		ArrayList <PlanetDefenceInfo> planetsInfo = new ArrayList <PlanetDefenceInfo>();
		for (Planet planet : myPlanets) {
			planetsInfo = addToList(planetsInfo, checkPlanetDefence(planet, fleet, orders, false));
		}

		return planetsInfo;
	}

    private static ArrayList<Fleet> getOrderedFleet(Planet planet, List<Fleet> fleet) {
        ArrayList <Fleet> inBoundFleetsOrdered = new ArrayList<Fleet>();
        for (Fleet f : fleet) {
            if (planet.PlanetID() == f.DestinationPlanet()) {
                if (planet.Owner() == 0) {
                    inBoundFleetsOrdered = addToListNeutral(inBoundFleetsOrdered, f);
                } else {
                    inBoundFleetsOrdered = addToList(inBoundFleetsOrdered, f);
                }
            }
        }
        return inBoundFleetsOrdered;
    }

    // analyse the defenses of a planet by considering it's current ships, growth rate and fleets bound to it
    // return the number of ships planet has when we land it if there is no fleets to arrive after our landing.
    // if there are enemy fleets to arrive after landing, then the number we need to hold will be returned.
    // - denotes if ships belong to us, + means enemy.
    public static int getAttackForceSize(Planet planet, List<Fleet> fleet, int distance, boolean sortedList, List<OrderInfo> orders) {
        List<Fleet> inBoundFleetsOrdered;
        if (!sortedList) {
            inBoundFleetsOrdered = getOrderedFleet(planet, fleet);
        } else {
            inBoundFleetsOrdered = fleet;
        }

		int strength = planet.NumShips();
        if (planet.Owner() == 1) {
            strength = getCurrentStrength(planet, orders);
            strength = -1*strength;
        }

        if (inBoundFleetsOrdered.size() == 0) {
            if (planet.Owner() == 2) {
                strength += planet.GrowthRate() * distance;
            } else {
                strength -= planet.GrowthRate() * distance;
            }
            return strength;
        }

		int timeElapsed = 0;
        boolean ownership = (planet.Owner() == 2);
        int mark = 0;
		for (Fleet f : inBoundFleetsOrdered) {
            int growth;
            int turnsRemaining = f.TurnsRemaining();
            if ((distance <= turnsRemaining) && (mark == 0)) {
                growth = planet.GrowthRate() * (distance - timeElapsed);
                if (ownership) {
                    strength += growth;
                } else {
                    strength -= growth;
                }
                if (distance == turnsRemaining) {
                    if (f.Owner() == 2) {
                        strength += f.NumShips();
                    } else {
                        strength -= f.NumShips();
                    }
                }
                mark = strength;        // we have reached the landing point. mark the strength needed at this point.
                strength = -1;            // now we own the enemy planet, new strength is -1
                strength -= planet.GrowthRate() * (turnsRemaining - distance);
                ownership = false;
            } else {
                growth = planet.GrowthRate() * (turnsRemaining - timeElapsed);

                if (ownership) {
                    strength += growth;
                } else {
                    strength -= growth;
                }
            }

            if (distance != turnsRemaining) {
                if (f.Owner() == 2) {
                    strength += f.NumShips();
                } else {
                    strength -= f.NumShips();
                }
            }

            if (strength < 0) {
                ownership = false;
            } else if (strength > 0) {     // mark what is needed to avoid this event.
                if (mark !=0) {
                    mark += strength+1;
                    strength = -1;
                    ownership = false;
                } else {
                    ownership = true;
                }
            }

            timeElapsed = turnsRemaining;
		}

        if (distance > timeElapsed) {
            if (ownership) {
                strength += planet.GrowthRate() * (distance - timeElapsed);
            } else {
                strength -= planet.GrowthRate() * (distance - timeElapsed);
            }
            return strength;
        } else {
            return mark;
        }
    }

    // analyse the defenses of an own planet by considering it's current ships, growth rate and fleets bound to it
    // return it's weakest point in time and the strength at that time.
    public static PlanetDefenceInfo checkPlanetDefence(Planet planet, List<Fleet> fleet, List<OrderInfo> orders, boolean sortedList) {
        List<Fleet> inBoundFleetsOrdered;
        if (!sortedList) {
            inBoundFleetsOrdered = getOrderedFleet(planet, fleet);
        } else {
            inBoundFleetsOrdered = fleet;
        }

		int strength = getCurrentStrength(planet, orders);
		int criticalTurn = 100;
		int timeElapsed = 0;
        int lowestStrength = strength;
        int lowestPoint = 100;
		for (Fleet f : inBoundFleetsOrdered) {
			int growth = planet.GrowthRate() * (f.TurnsRemaining()-timeElapsed);
			timeElapsed = f.TurnsRemaining();
			strength += growth;

			if (f.Owner() == 1) {
				strength += f.NumShips();
			} else {
				strength -= f.NumShips();
                if (strength < lowestStrength) {
                    lowestStrength = strength;
                    lowestPoint = timeElapsed;
                }
			}

			if (strength < 0) {
				criticalTurn = timeElapsed;
				break;
			}
		}

        if (strength >= 0) {
            criticalTurn = lowestPoint;
            strength = lowestStrength;
        }

		return new PlanetDefenceInfo(criticalTurn, strength, planet);
    }

    private static ArrayList<DefenceForceInfo> addToList(ArrayList<DefenceForceInfo> list, DefenceForceInfo defenceForceInfo) {
        int k=0;
        for (DefenceForceInfo force : list) {
            if ((force.distance < defenceForceInfo.distance) ||
                    ((force.distance == defenceForceInfo.distance) && (force.available > defenceForceInfo.available))) {
                k++;
            } else {
                break;
            }
        }

        if (k < list.size()) {
            list.add(k,defenceForceInfo);
        } else {
            list.add(defenceForceInfo);
        }

        return list;
    }

    //  0 - sort on growth only.
    //  1 - sort on growth, then on distance to a given planet.
    //  2 - sort on distance to a given planet, then on growth.
    //  3 - sort on distance per growth
    public static List<Planet> getSorted(PlanetWars pw, List <Planet>list, Planet closestEnemy, int mode) {
        List<Planet> planets = new ArrayList<Planet>();
        for (Planet p1 : list) {
            int k = 0;
            int distance1 = 0;
            if (closestEnemy != null) {
                distance1 = pw.Distance(closestEnemy, p1);
            }
            int growth = p1.GrowthRate();
            int distancePerGrowth = distance1 - 3*growth;
            for (Planet p2 : planets) {
                int distance2 = 0;
                if (closestEnemy != null) {
                    distance2 = pw.Distance(closestEnemy, p2);
                }
                boolean condition = false;
                if (mode == 0) {
                    condition = (growth < p2.GrowthRate()) ||
                        ((growth == p2.GrowthRate()) && (p1.NumShips() >= p2.NumShips()));
                } else if (mode == 1) {
                    condition = (growth < p2.GrowthRate()) ||
                        ((growth == p2.GrowthRate()) && (distance1 >= distance2));
                } else if (mode == 2) {
                    condition = (distance1 > distance2) ||
                        ((distance1 == distance2) && (growth <= p2.GrowthRate()));
                } else {
                     condition = (distancePerGrowth >= (distance2 - 3*p2.GrowthRate()));
                }

                if (condition) {
                    k++;
                } else {
                    break;
                }
            }

            if (k < planets.size()) {
                planets.add(k, p1);
            } else {
                planets.add(p1);
            }
        }

        return planets;
    }

    // orders neutral planets according to the growth rates which can be won per turn per ship
    public static ArrayList<Planet> addToList(PlanetWars pw, ArrayList <Planet>list, Planet p, Planet m, Planet e) {
		int k = 0;
        int mDistance = pw.Distance(p,m);
        int distance = pw.Distance(m,e);
        int distanceGapImpact = p.GrowthRate()*(distance - mDistance*2);
        int enemyImpact = e.NumShips() - distance*m.GrowthRate();
        if ((distanceGapImpact < 0) || enemyImpact <= 0) { // if this neutral is less than half the distance between me and enemy, it can help to have a smaller defence reserve against an enemy attack.
            distanceGapImpact = 0;
        }

		for (Planet n : list) {
            double thisInvestmentTime = (((double)p.NumShips())/p.GrowthRate()) + mDistance - distanceGapImpact; //time taken to produce back the investment
            double thisGrowthPerTurnPerShip = p.GrowthRate() / (thisInvestmentTime * (p.NumShips()+1));

            int existingDistance = pw.Distance(n,m);
            int existingGapImpact = distance - existingDistance * 2;
            if ((existingGapImpact < 0) || enemyImpact <= 0) { // if this neutral is less than half the distance between me and enemy, it can help to have a smaller defence reserve against an enemy attack.
                existingGapImpact = 0;
            }
            double existingInvestmentTime = (((double)n.NumShips())/n.GrowthRate()) + existingDistance - existingGapImpact;
            double existingGrowthPerTurnPerShip = n.GrowthRate() / (existingInvestmentTime * (n.NumShips()+1));

        if (Double.compare(thisGrowthPerTurnPerShip, existingGrowthPerTurnPerShip) < 0) {
				k++;
            } else {
				break;
			}
		}

        if (k<list.size()) {
            list.add(k,p);
        } else {
            list.add(p);
        }

		return list;
    }

    public static ArrayList<Fleet> addToList(ArrayList <Fleet>list, Fleet fleet) {
		int k = 0;
        boolean remove = false;
        Fleet newFleet = null;
        for (Fleet f : list) {
            int numShips = f.NumShips();
            int owner = f.Owner();
            int source = f.SourcePlanet();
            int tripLength = f.TotalTripLength();
			if (f.TurnsRemaining() < fleet.TurnsRemaining()) {
				k++;
			} else if (f.TurnsRemaining() == fleet.TurnsRemaining()){ //cancel out one
                if (owner == fleet.Owner()) {
                    numShips += fleet.NumShips(); //adding this to the existing fleet
                } else {
                    numShips -= fleet.NumShips();
                    if (numShips == 0) {
                        remove = true;
                        break; //remove this
                    } else if (numShips < 0) {
                        if (owner ==1) { // reverse the owner
                            owner = 2;
                        } else {
                            owner = 1;
                        }
                        numShips = Math.abs(numShips);
                        source = fleet.SourcePlanet();
                        tripLength = fleet.TotalTripLength();
                    }
                }
                newFleet = new Fleet(owner, numShips, source, f.DestinationPlanet(), tripLength, f.TurnsRemaining());
                break;
            } else {
				break;
			}
		}

        if (remove) {
            list.remove(k);
        } else if (newFleet != null) {
            list.set(k, newFleet);
        } else {
            if (k<list.size()) {
		        list.add(k,fleet);
            } else {
                list.add(fleet);
            }
        }

		return list;
    }

    // adds fleets to a list according to the arrival times.
    // unlike in owned planets, does not cancel out fleets opposing fleets.
    public static ArrayList<Fleet> addToListNeutral(ArrayList <Fleet>list, Fleet fleet) {
		int k = 0;
        boolean edit = false;
        int turnsRemaining = fleet.TurnsRemaining();
        int numShips = 0;
		for (Fleet f : list) {
            numShips = f.NumShips();
			if (f.TurnsRemaining() < turnsRemaining) {
				k++;
			} else if (f.TurnsRemaining() == turnsRemaining) {
                if (f.Owner() == fleet.Owner()) {
                    numShips += fleet.NumShips(); //adding this to the existing fleet
                    edit = true;
                    break;
                } else {
                    k++;
                }
            } else {
				break;
			}
		}

        if (edit) {
            Fleet combined = new Fleet(fleet.Owner(), numShips, fleet.SourcePlanet(),
                    fleet.DestinationPlanet(), fleet.TotalTripLength(), fleet.TurnsRemaining());
            list.set(k, combined);
        } else {
            if (k<list.size()) {
		        list.add(k,fleet);
            } else {
                list.add(fleet);
            }
        }
		return list;
    }

    public static ArrayList<PlanetDefenceInfo> addToList(ArrayList <PlanetDefenceInfo>list, PlanetDefenceInfo info) {
		int k = 0;
		for (PlanetDefenceInfo p : list) {
			if ((p.criticalPoint < info.criticalPoint) ||
                    ((p.criticalPoint == info.criticalPoint) && (p.strength >= info.strength))) {
				k++;
			} else {
				break;
			}
		}
        if (k<list.size()) {
            list.add(k,info);
        } else {
            list.add(info);
        }
		return list;
    }

    public static void main(String[] args) {
	String line = "";
	String message = "";
	int c;
	try {
	    while ((c = System.in.read()) >= 0) {
		switch (c) {
		case '\n':
		    if (line.equals("go")) {
			PlanetWars pw = new PlanetWars(message);
			DoTurn(pw);
		        pw.FinishTurn();
			message = "";
		    } else {
			message += line + "\n";
		    }
		    line = "";
		    break;
		default:
		    line += (char)c;
		    break;
		}
	    }
	} catch (Exception e) {
	    // Owned.
	}
    }
}


