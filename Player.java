import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Context {
    int factoryCount;
    int linkCount;
    int entityCount;
    List<Factory> enemyOrNeutralFactories;
    List<Factory> myFactories;
    List<Troop> enemyTroops;
    List<Troop> friendlyTroops;
    List<Link> links;
}

class Link {
    public Link(int factory1, int factory2, int distance){
        this.factory1 = factory1;
        this.factory2 = factory2;
        this.distance = distance;
        System.err.println(factory1 + " " + factory2 + " " + distance);
    }
    int factory1;
    int factory2;
    int distance;

    public Integer getDistance(){
        return this.distance;
    }
}

class Entity {
    int id;
    String type;
    int ownerCode;
    String owner;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

class Factory extends Entity {
    int numCyborgs;
    int factoryProduction;
    List<Link> links;

    public int getId(){
        return this.id;
    }
    public int getNumCyborgs(){
        return numCyborgs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

class Troop extends Entity {
    int factorySource;
    int factoryTarget;
    int numInTroop;
    int turnsBeforeArrival;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

class Bomb extends Entity {

}

class Constants {

    private Constants(){}
    public static final String ME = "ME";
    public static final String OPPONENT = "OPPONENT";
    public static final String NEUTRAL = "NEUTRAL";
    public static final String WAIT_COMMAND = "WAIT";
    public static final String FACTORY = "FACTORY";
    public static final String TROOP = "TROOP";

    static Map<Integer,String> ownerMap = Stream.of(
                    new AbstractMap.SimpleEntry<>(1,ME),
                    new AbstractMap.SimpleEntry<>(-1, OPPONENT),
                    new AbstractMap.SimpleEntry<>(0, NEUTRAL))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

}

class EntityArguments {

    int entityId;
    String entityType;
    int arg1;
    int arg2;
    int arg3;
    int arg4;
    int arg5;

    public EntityArguments(int entityId, String entityType, int arg1, int arg2, int arg3, int arg4, int arg5){
        this.entityId = entityId;
        this.entityType = entityType;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
        this.arg5 = arg5;
    }

}

class GameController {

    private final Scanner in;
    private Context context;

    public GameController(Scanner in){
        this.in = in;
    }

    public void run(){

        this.context = createContext();
        context.links = createLinks();
        while (in.hasNext() || in.hasNextInt()) {
            context.entityCount = getEntityCount();
            getEntities();

            System.out.println(getAction());
        }
    }

    private String getAction(){

        List<Target> targets =  context.enemyOrNeutralFactories.stream().map(
                factory -> {
                    Target target = new Target();
                    target.troopsInbound = Utility.countTroopsGoingTo(factory.id, context.friendlyTroops);
                    target.troopsPresent = factory.numCyborgs;
                    target.production = factory.factoryProduction;
                    target.factory = factory;
                    return target;
                }
        ).collect(Collectors.toList());
        System.err.println("# Targets: " + targets.size());

        List<Source> sources = context.myFactories.stream().map(
                factory -> {
                    Source source = new Source();
                    source.troopsInbound = Utility.countTroopsGoingTo(factory.id, context.enemyTroops);
                    source.troopsPresent = factory.numCyborgs;
                    source.factory = factory;
                    return source;
                }
        ).collect(Collectors.toList());
        System.err.println("# Sources: " + sources.size());

        sources.sort(Comparator.comparingInt(Source::getBaseNumberAvailable).reversed());

        return createAttacks(sources, targets);

    }

    private String createAttacks(List<Source> sources, List<Target> targets){

        if( sourcesOrTargetsAreEmpty(sources.isEmpty(),targets.isEmpty())){
            return Constants.WAIT_COMMAND;
        }

        List<String> commands = new ArrayList<>();

        for(Source source : sources){

            targets.forEach(target -> target.distance = Utility.getDistanceFromTo(source.factory.id, target.factory.id, context.links));

            Comparator<Target> compareByProdThenRqd = Comparator
                    .comparing(Target::getProduction).reversed()
                    .thenComparing(Target::getBaseNumberRequired)
                    .thenComparing(Target::getDistance);

            targets.sort(compareByProdThenRqd);

            int totalTargets = targets.size();
            int targetIndex = 0;

            if( troopsAvailableAmongAllSources(sources) ) {
                for (Target target : targets) {
                    if( sourceCanAttack(source)){
                        commands.add(attack(totalTargets, targetIndex, source, target));
                    } else {
                        System.err.println("Source: " + source.factory.id + " has zero troops available" );
                    }
                    targetIndex++;
                }
            } else {
                return Constants.WAIT_COMMAND;
            }
        }

        return String.join(";", commands);
    }

    private boolean sourcesOrTargetsAreEmpty(boolean sourcesIsEmpty, boolean targetsIsEmpty){
        return sourcesIsEmpty || targetsIsEmpty;
    }
    private boolean troopsAvailableAmongAllSources(List<Source> sources){
        int troopsAvailable = Utility.getTotalSourceTroops(sources);
        System.err.println("# troops available: " + troopsAvailable);
        return troopsAvailable > 1;
    }
    private boolean sourceCanAttack(Source source){
        return source.getBaseNumberAvailable() > 0;
    }
    private String attack(int totalTargets, int targetIndex, Source source, Target target  ){
        if( isLastFewFactories(totalTargets) || isLastTarget(targetIndex,totalTargets)){
            return sendMaxAvailable(source, target);
        }else if( isBombAppropriate(source, target)){
            return sendBomb(source, target);
        } else {
            return sendMinimumRequired(source, target);
        }
    }
    private boolean isLastFewFactories(int targetsSize){
        return targetsSize < 3;
    }
    private boolean isLastTarget(int targetIndex, int totalTargets){
        return targetIndex == totalTargets-1;
    }
    private boolean isBombAppropriate(Source source, Target target){
        return source.getBaseNumberAvailable() < 3 && target.getBaseNumberRequired() > 25 && target.production > 0;
    }
    private String sendBomb(Source source, Target target){
        System.err.println("BOMB: Source: " + source.factory.id + " Avl:" + source.getBaseNumberAvailable() + " Target: " + target.factory.id + " Rqd:" + target.getBaseNumberRequired() );
        return "BOMB " + source.factory.id + " " + target.factory.id;
    }
    private String sendMinimumRequired(Source source, Target target){
        System.err.println("Source: " + source.factory.id + " Avl:" + source.getBaseNumberAvailable() + " Target: " + target.factory.id + " Rqd:" + target.getBaseNumberRequired() );
        return "MOVE " + source.factory.id + " " + target.factory.id + " " + Utility.getTroopCountToSend(source, target);
    }
    private String sendMaxAvailable(Source source, Target target){
        System.err.println("Source: " + source.factory.id + " Avl:" + source.getBaseNumberAvailable() + " Target: " + target.factory.id + " Rqd:" + target.getBaseNumberRequired() );
        return ("MOVE " + source.factory.id + " " + target.factory.id + " " + Utility.getMaxTroopsToSend(source, target));
    }


    private Context createContext(){
        this.context = new Context();
        context.factoryCount = in.nextInt();
        context.linkCount = in.nextInt();
        return context;
    }

    private List<Link> createLinks(){
        List<Link> links = new ArrayList<>(context.linkCount);
        for (int i = 0; i < context.linkCount; i++) {
            int factory1 = in.nextInt();
            int factory2 = in.nextInt();
            int distance = in.nextInt();
            links.add(new Link(factory1, factory2, distance));
        }
        return links;
    }

    private int getEntityCount(){
        return in.nextInt();
    }

    private void getEntities(){
        List<Factory> enemyOrNeutral = new ArrayList<>();
        List<Factory> myFactories = new ArrayList<>();
        List<Troop> enemyTroops = new ArrayList<>();
        List<Troop> friendlyTroops = new ArrayList<>();

        for (int i = 0; i < this.context.entityCount; i++) {
            EntityArguments args = new EntityArguments(in.nextInt(),in.next(),in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt() );

            if ( isFactory(args.entityType)){
                Factory factory = createFactory(args);
                factory.links = getLinksForFactory(factory.id);
                if(factory.owner.equalsIgnoreCase(Constants.ME)){
                    myFactories.add(factory);
                } else {
                    enemyOrNeutral.add(factory);
                }
            } else if(isTroop(args.entityType)){
                Troop troop = createTroop(args);
                if(troop.owner.equalsIgnoreCase(Constants.ME)){
                    friendlyTroops.add(troop);
                } else {
                    enemyTroops.add(troop);
                }
            }
        }
        context.myFactories = myFactories;
        context.enemyOrNeutralFactories = enemyOrNeutral;
        context.enemyTroops = enemyTroops;
        context.friendlyTroops = friendlyTroops;
    }

    private Factory createFactory(EntityArguments args){
        Factory factory = new Factory();
        factory.id = args.entityId;
        factory.type = args.entityType;
        factory.ownerCode = args.arg1;
        factory.owner = Constants.ownerMap.get(factory.ownerCode);
        factory.numCyborgs = args.arg2;
        factory.factoryProduction = args.arg3;
        return factory;
    }

    private Troop createTroop(EntityArguments args){
        Troop troop = new Troop();
        troop.id = args.entityId;
        troop.type = args.entityType;
        troop.ownerCode = args.arg1;
        troop.owner = Constants.ownerMap.get(troop.ownerCode);
        troop.factorySource = args.arg2;
        troop.factoryTarget = args.arg3;
        troop.numInTroop = args.arg4;
        troop.turnsBeforeArrival = args.arg5;
        return troop;
    }

    private List<Link> getLinksForFactory(int factoryId){
        return context.links.stream()
                .filter( l -> l.factory1 == factoryId || l.factory2 == factoryId).collect(Collectors.toList());
    }

    private boolean isFactory(String entityType){
        return entityType.equalsIgnoreCase(Constants.FACTORY);
    }
    private boolean isTroop(String entityType){
        return entityType.equalsIgnoreCase(Constants.TROOP);
    }
}

enum Focus {
    ATTACK,
    DEFEND,
    SEEK_SUPPORT,
    SEND_SUPPORT
}

class Target {
    int troopsInbound;
    int troopsPresent;
    int production;
    int distance;
    Factory factory;

    public int getDistance(){
        return distance;
    }
    public int getProduction(){
        return production;
    }
    public int getBaseNumberRequired(){
        return troopsPresent - troopsInbound + 1;
    }
}

class Source {
    int troopsInbound;
    int troopsPresent;
    int production;
    int troopsSent;
    Focus focus;
    Factory factory;

    public int getBaseNumberAvailable(){
        return Math.max( troopsPresent - troopsInbound - troopsSent - 1, 0);
    }
}

class Utility {
    private Utility(){}

    public static int countTroopsGoingTo(int targetId, List<Troop> troopList){

        return troopList.stream()
                .filter( troop -> troop.factoryTarget == targetId)
                .map( troop -> troop.numInTroop)
                .reduce(Integer::sum)
                .orElse(0);

    }

    public static int getDistanceFromTo(int fromId, int toId, List<Link> links){
        Optional<Link> targetLink = links.stream()
                .filter( link -> (link.factory1 == fromId && link.factory2 == toId) || (link.factory2 == fromId && link.factory1 == toId) )
                .findFirst();

        return targetLink.map(link -> link.distance).orElse(100);

    }

    public static int getTotalSourceTroops(List<Source> sources){
        return sources.stream()
                .map(Source::getBaseNumberAvailable)
                .reduce(Integer::sum).orElse(0);
    }

    public static int getTroopCountToSend(Source source, Target target){
        if( source.getBaseNumberAvailable() > 0 && target.getBaseNumberRequired() > 0 ){
            int sending = Math.min(source.getBaseNumberAvailable(), target.getBaseNumberRequired());
            source.troopsSent += sending;
            target.troopsInbound += sending;
            return sending;
        } else {
            return 0;
        }
    }

    public static int getMaxTroopsToSend(Source source, Target target){
        if( source.getBaseNumberAvailable() > 0 && target.getBaseNumberRequired() > 0 ){
            int sending = source.getBaseNumberAvailable();
            source.troopsSent += sending;
            target.troopsInbound += sending;
            return sending;
        } else {
            return 0;
        }
    }
}
class Player {

    public static void main(String[] args) {

            Scanner in = new Scanner(System.in);
            GameController controller = new GameController(in);
            controller.run();
    }

}