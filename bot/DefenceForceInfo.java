
public class DefenceForceInfo {
    public int available;
    public int force;
    public PlanetDefenceInfo planetInfo;
    public int index;
    public int distance;

    public DefenceForceInfo(int j, PlanetDefenceInfo sourceInfo, int distance, int i, int strength) {
        index = j;
        planetInfo = sourceInfo;
        this.distance = distance;
        available = strength;
        force = i;
    }
}
