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
}

class Troop extends Entity {
    int factorySource;
    int factoryTarget;
    int numInTroop;
    int turnsBeforeArrival;
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

class StrategyBuilder{

    public List<Factory> findAttackingFactories(List<Factory> factories){
        return factories.stream().filter( f -> f.numCyborgs > 3).collect(Collectors.toList());
    }

    public Factory findFactoryWithMostCyborgs(List<Factory> factories){
        if (factories.isEmpty()) throw new AssertionError("Factories collection is empty");
        Factory mostCyborgs =  factories.stream()
                .max(Comparator.comparing(Factory::getNumCyborgs))
                .orElse(factories.get(0));

        System.err.println("My factory with most cyborgs is: " + mostCyborgs.id + " which has " + mostCyborgs.numCyborgs + " borgs");
        return mostCyborgs;
    }

    public Factory findClosestFactory(Factory factory, List<Factory> myFactories, List<Factory> enemyFactories){
        if(Objects.isNull(factory)) throw new AssertionError("Factory is null");
        if (enemyFactories.isEmpty()) throw new AssertionError("Factories collection is empty");

        List<Integer> myFactoryIds = getMyFactoryIds(myFactories);
        List<Link> linksToEnemies = getLinksToEnemies(factory, myFactoryIds);
        Link closestLink = closestLink(linksToEnemies);

        System.err.println("Closest link to factory " + factory.id + " is " + closestLink.factory1 + " " + closestLink.factory2 + " " + closestLink.distance );

        Factory closestEnemy = getClosestEnemy(enemyFactories, closestLink);

        System.err.println("Closest enemy factory to target is " + closestEnemy.id + " with a distance of " + closestLink.distance);
        return closestEnemy;
    }

    private Factory getClosestEnemy(List<Factory> enemyFactories, Link closestLink){
        return enemyFactories.stream()
                .filter( f -> f.id == closestLink.factory1 || f.id == closestLink.factory2 )
                .findFirst()
                .orElse(enemyFactories.get(0));
    }

    private List<Link> getLinksToEnemies(Factory factory, List<Integer> myFactoryIds){
        return factory.links.stream().filter( l -> !myFactoryIds.contains(l.factory1) || !myFactoryIds.contains(l.factory2) ).collect(Collectors.toList());
    }

    private Link closestLink(List<Link> links){
        return links.stream()
                .min( Comparator.comparing(Link::getDistance))
                .orElse(links.get(0));
    }

    private List<Integer> getMyFactoryIds(List<Factory> factories){
        return factories.stream().map(Factory::getId).collect(Collectors.toList());
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

        // get targets by how many troops they need in ascending order (present there + accum by arrival - minus outgoing)
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

        // get sources by how many troops are available (present - incoming - reserve)
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

        targets.sort(Comparator.comparingInt(Target::getBaseNumberRequired));
        sources.sort(Comparator.comparingInt(Source::getBaseNumberAvailable).reversed());

        return createAttacks(sources, targets);

    }

    private String createAttacks(List<Source> sources, List<Target> targets){

        if(sources.isEmpty() || targets.isEmpty()){
            return Constants.WAIT_COMMAND;
        }

        List<String> commands = new ArrayList<>();

        for(Source source : sources){
            int troopsAvailable = Utility.getTotalSourceTroops(sources);
            System.err.println("# troops available: " + troopsAvailable);
            if(troopsAvailable > 1 ) {
                for (Target target : targets) {
                    if( source.getBaseNumberAvailable() > 0){
                        if( targets.size() < 3){
                            System.err.println("Source: " + source.factory.id + " Avl:" + source.getBaseNumberAvailable() + " Target: " + target.factory.id + " Rqd:" + target.getBaseNumberRequired() );
                            String command = "MOVE " + source.factory.id + " " + target.factory.id + " " + Utility.getMaxTroopsToSend(source, target);
                            commands.add(command);
                        }else {
                            if(source.getBaseNumberAvailable() < 3 && target.getBaseNumberRequired() > 25 && target.factory.factoryProduction > 0){
                                System.err.println("BOMB: Source: " + source.factory.id + " Avl:" + source.getBaseNumberAvailable() + " Target: " + target.factory.id + " Rqd:" + target.getBaseNumberRequired() );
                                String command = "BOMB " + source.factory.id + " " + target.factory.id;
                                commands.add(command);
                            } else {
                                System.err.println("Source: " + source.factory.id + " Avl:" + source.getBaseNumberAvailable() + " Target: " + target.factory.id + " Rqd:" + target.getBaseNumberRequired() );
                                String command = "MOVE " + source.factory.id + " " + target.factory.id + " " + Utility.getTroopCountToSend(source, target);
                                commands.add(command);
                            }
                        }
                    } else {
                        System.err.println("Source: " + source.factory.id + " has zero troops available" );
                    }
                }
            } else {
                return Constants.WAIT_COMMAND;
            }
        }

        return commands.stream().collect(Collectors.joining(";"));
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

class Target {
    int troopsInbound;
    int troopsPresent;
    int production;
    Factory factory;

    public int getBaseNumberRequired(){
        return troopsPresent - troopsInbound + 1;
    }
}

class Source {
    int troopsInbound;
    int troopsPresent;
    int production;
    int troopsSent;
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
                .reduce( (cum, temp) -> cum + temp )
                .orElse(0);

    }

    public static int getDistanceFromTo(int fromId, int toId, List<Link> links){
        Optional<Link> targetLink = links.stream()
                .filter( link -> {
                    return (link.factory1 == fromId && link.factory2 == toId)
                            || (link.factory2 == fromId && link.factory1 == toId);
                }).findFirst();

        return targetLink.map(link -> link.distance).orElse(100);

    }

    public static int getTotalSourceTroops(List<Source> sources){
        return sources.stream()
                .map( source -> source.getBaseNumberAvailable() )
                .reduce( (cum, temp) -> cum + temp).orElse(0);
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