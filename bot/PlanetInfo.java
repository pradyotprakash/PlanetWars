import java.util.List;

public class PlanetInfo {
    public Planet planet;
    public Planet closestEnemy;
    public int distanceToClosestEnemy;
    public List<Fleet> fleets; // all fleets heading to this planet, ordered time wise
    public List <Integer> ships; //number of ships at each turn upto all the current fleets landed

    // if the nearest enemy launched a full scale attack, what would we left with
    // after neutralising it. including the fleets already in motion.
    public int safetyLevel2;

    // what is the extra we would left with after allowing for all fleets in motion 
    public int safetyLevel1;

    public PlanetInfo(Planet closestEnemy, int distance, List <Fleet> fleets) {
        this.closestEnemy = closestEnemy;
        this.distanceToClosestEnemy = distance;
        this.fleets = fleets;
        forecast();
        setSafetyLevels();
    }

    private void setSafetyLevels() {
        safetyLevel1 = planet.NumShips();
        for (Integer i : ships) {
            if (i < safetyLevel1) {
                safetyLevel1 = i;
            }
        }

        safetyLevel2 = ships.get(distanceToClosestEnemy) - closestEnemy.NumShips();
    }

    private void forecast() {
        int currentShips = planet.NumShips();
        ships.add(currentShips);
        int currentStep = 0;
        boolean ownership = true;
        for (Fleet f:fleets) {
            for (int i=0; i<f.TurnsRemaining(); i++) {
                if (ownership) {
                    currentShips += planet.GrowthRate();
                } else {
                    currentShips -= planet.GrowthRate();
                }
                ships.add(currentShips);
                currentStep++;
            }

            if (f.Owner() == 1) {
                currentShips += f.NumShips();
            } else {
                currentShips -= f.NumShips();
            }
            
            ships.set(currentStep, currentShips);
            if (currentShips > 0) {  // todo fleets arriving at the same time. if ships==0
                ownership = true;
            } else {
                ownership = false;
            }
        }
    }
}
