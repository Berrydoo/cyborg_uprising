import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Context {
    int factoryCount;
    int linkCount;
    int entityCount;
    List<Factory> factoryList;
    List<Troop> troopList;
    List<Link> links;
}

class Link {
    public Link(int factory1, int factory2, int distance){
        this.factory1 = factory1;
        this.factory2 = factory2;
        this.distance = distance;
    }
    int factory1;
    int factory2;
    int distance;
}

class Entity {
    int id;
    String type;
    int ownerCode;
    String owner;
}

class Factory extends Entity {
    int numCyborgs;
    int factoryProduction;
}

class Troop extends Entity {
    int factorySource;
    int factoryTarget;
    int numInTroop;
    int turnsBeforeArrival;
}

class Constants {

    private Constants(){}

    static Map<Integer,String> ownerMap = Stream.of(
                    new AbstractMap.SimpleEntry<>(1,"ME"),
                    new AbstractMap.SimpleEntry<>(-1, "OPPONENT"),
                    new AbstractMap.SimpleEntry<>(0, "NEUTRAL"))
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

class Player {

    public static void main(String[] args) {

            Scanner in = new Scanner(System.in);
            Context context = createContext(in);
            context.links = createLinks(context, in);

            // game loop
            while (in.hasNext() || in.hasNextInt()) {
                context.entityCount = getEntityCount(in); // the number of entities (e.g. factories and troops)
                context = getEntities(context, in);

                // Any valid action, such as "WAIT" or "MOVE source destination cyborgs"
                System.out.println(getAction(context));
            }

    }

    public static String getAction(Context context){
        return "WAIT";
    }

    public static Context createContext(Scanner in){
        Context context = new Context();
        context.factoryCount = in.nextInt();
        context.linkCount = in.nextInt();
        return context;
    }

    public static List<Link> createLinks(Context context, Scanner in){
        List<Link> links = new ArrayList<>(context.linkCount);
        for (int i = 0; i < context.linkCount; i++) {
            int factory1 = in.nextInt();
            int factory2 = in.nextInt();
            int distance = in.nextInt();
            links.add(new Link(factory1, factory2, distance));
        }
        return links;
    }

    public static int getEntityCount(Scanner in){
        return in.nextInt();
    }

    public static Context getEntities(Context context, Scanner in){
        List<Factory> factories = new ArrayList<>();
        List<Troop> troops = new ArrayList<>();

        for (int i = 0; i < context.entityCount; i++) {

            EntityArguments args = new EntityArguments(in.nextInt(),in.next(),in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt() );

            if ( isFactory(args.entityType)){
                factories.add(createFactory(args));
            } else if(isTroop(args.entityType)){
                troops.add(createTroop(args));
            }
        }
        context.factoryList = factories;
        context.troopList = troops;

        return context;
    }

    private static Factory createFactory(EntityArguments args){
        Factory factory = new Factory();
        factory.id = args.entityId;
        factory.type = args.entityType;
        factory.ownerCode = args.arg1;
        factory.owner = Constants.ownerMap.get(factory.ownerCode);
        factory.numCyborgs = args.arg2;
        factory.factoryProduction = args.arg3;
        return factory;
    }

    private static Troop createTroop(EntityArguments args){
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

    private static boolean isFactory(String entityType){
        return entityType.equalsIgnoreCase("FACTORY");
    }
    private static boolean isTroop(String entityType){
        return entityType.equalsIgnoreCase("TROOP");
    }
}