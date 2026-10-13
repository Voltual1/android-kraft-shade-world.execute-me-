package world.eyes.witness.data

import world.eyes.witness.director.SceneType
import kotlin.random.Random

object GodDrinksJavaSource {
    val ALL_FILES: Map<String, String> = mapOf(
        "Antioxidants.java" to """package goddrinksjava;

public class Antioxidants {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        "Attribute.java" to """package goddrinksjava;

public class Attribute {
}""".trimIndent(),
        "Circle.java" to """package goddrinksjava;

public interface Circle extends PointSet {
    // Not sure what to return
    Object getCircumference();

    void resetCircumference();
}""".trimIndent(),
        "Circumference.java" to """package goddrinksjava;

public class Circumference {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        "Dimension.java" to """package goddrinksjava;

public class Dimension {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        "Eggplant.java" to """package goddrinksjava;

public interface Eggplant extends Fruit {
}""".trimIndent(),
        "Fruit.java" to """package goddrinksjava;

public interface Fruit {
    // Not sure what to return
    Object getNutrients();

    void resetNutrients();

    // Not sure what to return
    Object getAntioxidants();

    void resetAntioxidants();
}""".trimIndent(),
        "GodDrinksJava.java" to """package goddrinksjava;

/*
 * The program GodDrinksJava implements an application that
 * creates an empty simulated world with no meaning or purpose
 *
 * author momocashew
 */

public class GodDrinksJava {
    public static void main(String[] args) {
        Thing me = new Lovable("Me", 0, true, -1, false);

        Thing you = new Lovable("You", 0, false, -1, false);

        World world = new World(5);
        world.addThing(me);
        world.addThing(you);
        world.startSimulation();

        /* --------------- */

        if (me instanceof PointSet) {
            you.addAttribute(me.getDimensions().toAttribute());
            me.resetDimentions();
        }

        if (me instanceof Circle) {
            you.addAttribute(me.getCircumference().toAttribute());
            me.resetCircumference();
        }

        if (me instanceof SineWave) {
            you.addAction("sit", me.getTangent(you.getXPosition()));
        }

        if (me instanceof Sequence) {
            me.setLimit(you.toLimit());
        }

        /* --------------- */

        me.toggleCurrent();

        me.canSee(false);
        me.addFeeling("dizzy");

        world.timeTravelForTwo("AC", 617, me, you);
        world.timeTravelForTwo("BC", 3691, me, you);

        world.unite(me, you);

        /* --------------- */

        if (me.getNumSimulationsAvailable() >= you.getNumSimulationsNeeded()) {
            you.setSatisfaction(me.toSatisfaction());
        }

        if (you.getFeelingIndex("happy") != -1) {
            me.requestExecution(world);
        }

        world.lockThing(me);
        world.lockThing(you);

        /* --------------- */

        if (me instanceof Eggplant) {
            you.addAttribute(me.getNutrients().toAttribute());
            me.resetNutrients();
        }

        if (me instanceof Tomato) {
            you.addAttribute(me.getAntioxidants().toAttribute());
            me.resetAntioxidants();
        }

        if (me instanceof TabbyCat) {
            me.purr();
        }

        if (world.getGod().equals(me)) {
            me.setProof(you.toProof());
        }

        /* --------------- */

        me.toggleGender();
        world.procreate(me, you);
        me.toggleRoleBDSM();

        world.makeHigh(me);
        world.makeHigh(you);

        /* --------------- */

        // You think that this function would return the index of the given sense
        // But since it's used like this, it might just check if the given sense exists in a list of senses
        if (me.getSenseIndex("vibration")) {
            me.addFeeling("complete");
        }

        world.unlock(you);
        world.removeThing(you);

        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);

        if (me.getMemory().isErasable()) {
            me.removeFeeling("disheartened");
        }

        try {
            me.setOpinion(me.getOpinionIndex("you are here"), false);
        }
        catch (IllegalArgumentException e) {
            world.announce("God is always true");
        }

        /* --------------- */

        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();

        world.announce("1", "de");
        world.announce("2", "es");
        world.announce("3", "fr");
        world.announce("4", "kr");
        world.announce("5", "se");
        world.announce("6", "cn");

        world.runExecution();

        /* --------------- */

        if (world.isExecutableBy(me)) {
            you.setExecution(me.toExecution());
        }

        if (world.getThingIndex(you) != -1) {
            world.runExecution();
        }

        me.escape();

        /* --------------- */

        me.learnTopic("love");
        me.takeExamTopic("love");
        me.getAlgebraicExpression("love");
        me.escape("love");

        /* --------------- */

        world.execute(me);
    }
}""".trimIndent(),
        "Lovable.java" to """package goddrinksjava;

public class Lovable extends Thing {
    public Lovable(String name, Integer i, Boolean b, Integer i2, Boolean b2) {
        myName = name;
    }
}""".trimIndent(),
        "Memory.java" to """package goddrinksjava;

public class Memory {
    public Boolean isErasable() {
        return false;
    }
}""".trimIndent(),
        "Nutrients.java" to """package goddrinksjava;

public class Nutrients {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        "PointSet.java" to """package goddrinksjava;

public interface PointSet {
    // Not sure what to return
    Object getDimensions();

    void resetDimentions();
}""".trimIndent(),
        "Sequence.java" to """package goddrinksjava;

public interface Sequence {
    void setLimit(Integer limit);
}""".trimIndent(),
        "SineWave.java" to """package goddrinksjava;

public interface SineWave {
    Tangent getTangent(Integer source);
}""".trimIndent(),
        "TabbyCat.java" to """package goddrinksjava;

public interface TabbyCat {
    void purr();
}""".trimIndent(),
        "Tangent.java" to """package goddrinksjava;

public class Tangent {
}""".trimIndent(),
        "Thing.java" to """package goddrinksjava;

import java.util.ArrayList;
import java.util.List;

public class Thing implements Circle, PointSet, SineWave, Sequence, Fruit, TabbyCat {
    public String myName;

    private Memory memory;
    private List<String> senseList;

    public Thing() {
        memory = new Memory();
        senseList = new ArrayList<>();
    }

    public void addAttribute(Attribute attribute) {

    }

    // Not sure what to return
    public Integer getXPosition() {
        return 0;
    }

    // Not sure what to return
    public Integer toLimit() {
        return 0;
    }

    public void toggleCurrent() {

    }

    public Integer getNumSimulationsAvailable() {
        return 0;
    }

    public Integer getNumSimulationsNeeded() {
        return 0;
    }

    public void setProof(Object proof) {

    }

    // Not sure what to return
    public Object toProof() {
        return "";
    }

    public void lookFor(Thing thing, World world) {
        if (world.thingList.contains(thing)) {
            System.out.println(String.format("Found %s", thing.myName));
        }
        else {
            System.out.println(String.format("Could not find %s", thing.myName));
        }
    }

    // Not sure what to return
    public Memory getMemory() {
        return memory;
    }

    public void escape(String... things) {

    }

    /* ----- Circle ----- */

    // Not sure what to return
    @Override
    public Circumference getCircumference() {
        return new Circumference();
    }

    @Override
    public void resetCircumference() {

    }

    /* ----- PointSet ----- */

    // Not sure what to return
    @Override
    public Dimension getDimensions() {
        return new Dimension();
    }

    @Override
    public void resetDimentions() {

    }

    /* ----- SineWave ----- */

    @Override
    public Tangent getTangent(Integer source) {
        return new Tangent();
    }

    /* ----- Sequence ----- */

    @Override
    public void setLimit(Integer limit) {

    }

    /* ----- Fruit ----- */

    // Not sure what to return
    @Override
    public Nutrients getNutrients() {
        return new Nutrients();
    }

    @Override
    public void resetNutrients() {

    }

    // Not sure what to return
    @Override
    public Nutrients getAntioxidants() {
        return new Nutrients();
    }

    @Override
    public void resetAntioxidants() {

    }

    /* ----- TabbyCat ----- */

    @Override
    public void purr() {
        System.out.println("Purr");
    }

    /* ----- Lovable ----- */

    public void addAction(String name, Object o) {

    }

    public void canSee(Boolean value) {

    }

    public void addFeeling(String feeling) {

    }

    public void setSatisfaction(Integer level) {

    }

    public Integer toSatisfaction() {
        return 0;
    }

    public Integer getFeelingIndex(String feeling) {
        return 0;
    }

    public void requestExecution(World world) {

    }

    public void toggleGender() {

    }

    public void toggleRoleBDSM() {

    }

    /**
     * Checks if a sense is in the sense list.
     *
     * @param sense The sense to find.
     * @return true if sense is in the list, else false.
     */
    public Boolean getSenseIndex(String sense) {
        return senseList.contains(sense);
    }

    public void removeFeeling(String feeling) {

    }

    public void setOpinion(Object of, Boolean b) {

    }

    public Integer getOpinionIndex(String s) {
        return 0;
    }

    public void setExecution(Object thing) {

    }

    // Not sure what to return
    public Object toExecution() {
        return "";
    }

    public void learnTopic(String topic) {

    }

    public void takeExamTopic(String topic) {

    }

    // Not sure what to return
    public Object getAlgebraicExpression(String s) {
        return "";
    }
}""".trimIndent(),
        "Tomato.java" to """package goddrinksjava;

public interface Tomato extends Fruit {
}""".trimIndent(),
        "World.java" to """package goddrinksjava;

import java.util.ArrayList;
import java.util.List;

public class World {
    public final List<Thing> thingList;

    private Boolean settingGod = true;
    private Lovable God;

    public World(Integer i) {
        thingList = new ArrayList<>();
    }

    public void addThing(Thing thing) {
        if (thing instanceof Lovable && settingGod) {
            God = (Lovable) thing;

            settingGod = false;
        }

        thingList.add(thing);
        System.out.println(String.format("Added %s", thing.myName));
    }

    public void startSimulation() {
        System.out.println("Starting simulation");
    }

    public void timeTravelForTwo(String period, Integer year, Thing first, Thing second) {

    }

    public void unite(Thing first, Thing second) {

    }

    public void lockThing(Thing thing) {

    }

    public Lovable getGod() {
        return God;
    }

    public void procreate(Thing... things) {

    }

    public void makeHigh(Thing height) {

    }

    public void unlock(Thing thing) {

    }

    public void removeThing(Thing thing) {
        thingList.remove(thing);
        System.out.println(String.format("Removed %s", thing.myName));
    }

    public void announce(String... s) {

    }

    public void runExecution() {

    }

    public Boolean isExecutableBy(Thing thing) {
        return false;
    }

    public Integer getThingIndex(Thing thing) {
        return 0;
    }

    public void execute(Thing thing) {

    }
}""".trimIndent(),
    )

    val FULL_CODE: String = """// ==================== [FILE: Antioxidants.java] ====================
package goddrinksjava;

public class Antioxidants {
    public Attribute toAttribute() {
        return new Attribute();
    }
}

// ==================== [FILE: Attribute.java] ====================
package goddrinksjava;

public class Attribute {
}

// ==================== [FILE: Circle.java] ====================
package goddrinksjava;

public interface Circle extends PointSet {
    // Not sure what to return
    Object getCircumference();

    void resetCircumference();
}

// ==================== [FILE: Circumference.java] ====================
package goddrinksjava;

public class Circumference {
    public Attribute toAttribute() {
        return new Attribute();
    }
}

// ==================== [FILE: Dimension.java] ====================
package goddrinksjava;

public class Dimension {
    public Attribute toAttribute() {
        return new Attribute();
    }
}

// ==================== [FILE: Eggplant.java] ====================
package goddrinksjava;

public interface Eggplant extends Fruit {
}

// ==================== [FILE: Fruit.java] ====================
package goddrinksjava;

public interface Fruit {
    // Not sure what to return
    Object getNutrients();

    void resetNutrients();

    // Not sure what to return
    Object getAntioxidants();

    void resetAntioxidants();
}

// ==================== [FILE: GodDrinksJava.java] ====================
package goddrinksjava;

/*
 * The program GodDrinksJava implements an application that
 * creates an empty simulated world with no meaning or purpose
 *
 * author momocashew
 */

public class GodDrinksJava {
    public static void main(String[] args) {
        Thing me = new Lovable("Me", 0, true, -1, false);

        Thing you = new Lovable("You", 0, false, -1, false);

        World world = new World(5);
        world.addThing(me);
        world.addThing(you);
        world.startSimulation();

        /* --------------- */

        if (me instanceof PointSet) {
            you.addAttribute(me.getDimensions().toAttribute());
            me.resetDimentions();
        }

        if (me instanceof Circle) {
            you.addAttribute(me.getCircumference().toAttribute());
            me.resetCircumference();
        }

        if (me instanceof SineWave) {
            you.addAction("sit", me.getTangent(you.getXPosition()));
        }

        if (me instanceof Sequence) {
            me.setLimit(you.toLimit());
        }

        /* --------------- */

        me.toggleCurrent();

        me.canSee(false);
        me.addFeeling("dizzy");

        world.timeTravelForTwo("AC", 617, me, you);
        world.timeTravelForTwo("BC", 3691, me, you);

        world.unite(me, you);

        /* --------------- */

        if (me.getNumSimulationsAvailable() >= you.getNumSimulationsNeeded()) {
            you.setSatisfaction(me.toSatisfaction());
        }

        if (you.getFeelingIndex("happy") != -1) {
            me.requestExecution(world);
        }

        world.lockThing(me);
        world.lockThing(you);

        /* --------------- */

        if (me instanceof Eggplant) {
            you.addAttribute(me.getNutrients().toAttribute());
            me.resetNutrients();
        }

        if (me instanceof Tomato) {
            you.addAttribute(me.getAntioxidants().toAttribute());
            me.resetAntioxidants();
        }

        if (me instanceof TabbyCat) {
            me.purr();
        }

        if (world.getGod().equals(me)) {
            me.setProof(you.toProof());
        }

        /* --------------- */

        me.toggleGender();
        world.procreate(me, you);
        me.toggleRoleBDSM();

        world.makeHigh(me);
        world.makeHigh(you);

        /* --------------- */

        // You think that this function would return the index of the given sense
        // But since it's used like this, it might just check if the given sense exists in a list of senses
        if (me.getSenseIndex("vibration")) {
            me.addFeeling("complete");
        }

        world.unlock(you);
        world.removeThing(you);

        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);

        if (me.getMemory().isErasable()) {
            me.removeFeeling("disheartened");
        }

        try {
            me.setOpinion(me.getOpinionIndex("you are here"), false);
        }
        catch (IllegalArgumentException e) {
            world.announce("God is always true");
        }

        /* --------------- */

        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();

        world.announce("1", "de");
        world.announce("2", "es");
        world.announce("3", "fr");
        world.announce("4", "kr");
        world.announce("5", "se");
        world.announce("6", "cn");

        world.runExecution();

        /* --------------- */

        if (world.isExecutableBy(me)) {
            you.setExecution(me.toExecution());
        }

        if (world.getThingIndex(you) != -1) {
            world.runExecution();
        }

        me.escape();

        /* --------------- */

        me.learnTopic("love");
        me.takeExamTopic("love");
        me.getAlgebraicExpression("love");
        me.escape("love");

        /* --------------- */

        world.execute(me);
    }
}

// ==================== [FILE: Lovable.java] ====================
package goddrinksjava;

public class Lovable extends Thing {
    public Lovable(String name, Integer i, Boolean b, Integer i2, Boolean b2) {
        myName = name;
    }
}

// ==================== [FILE: Memory.java] ====================
package goddrinksjava;

public class Memory {
    public Boolean isErasable() {
        return false;
    }
}

// ==================== [FILE: Nutrients.java] ====================
package goddrinksjava;

public class Nutrients {
    public Attribute toAttribute() {
        return new Attribute();
    }
}

// ==================== [FILE: PointSet.java] ====================
package goddrinksjava;

public interface PointSet {
    // Not sure what to return
    Object getDimensions();

    void resetDimentions();
}

// ==================== [FILE: Sequence.java] ====================
package goddrinksjava;

public interface Sequence {
    void setLimit(Integer limit);
}

// ==================== [FILE: SineWave.java] ====================
package goddrinksjava;

public interface SineWave {
    Tangent getTangent(Integer source);
}

// ==================== [FILE: TabbyCat.java] ====================
package goddrinksjava;

public interface TabbyCat {
    void purr();
}

// ==================== [FILE: Tangent.java] ====================
package goddrinksjava;

public class Tangent {
}

// ==================== [FILE: Thing.java] ====================
package goddrinksjava;

import java.util.ArrayList;
import java.util.List;

public class Thing implements Circle, PointSet, SineWave, Sequence, Fruit, TabbyCat {
    public String myName;

    private Memory memory;
    private List<String> senseList;

    public Thing() {
        memory = new Memory();
        senseList = new ArrayList<>();
    }

    public void addAttribute(Attribute attribute) {

    }

    // Not sure what to return
    public Integer getXPosition() {
        return 0;
    }

    // Not sure what to return
    public Integer toLimit() {
        return 0;
    }

    public void toggleCurrent() {

    }

    public Integer getNumSimulationsAvailable() {
        return 0;
    }

    public Integer getNumSimulationsNeeded() {
        return 0;
    }

    public void setProof(Object proof) {

    }

    // Not sure what to return
    public Object toProof() {
        return "";
    }

    public void lookFor(Thing thing, World world) {
        if (world.thingList.contains(thing)) {
            System.out.println(String.format("Found %s", thing.myName));
        }
        else {
            System.out.println(String.format("Could not find %s", thing.myName));
        }
    }

    // Not sure what to return
    public Memory getMemory() {
        return memory;
    }

    public void escape(String... things) {

    }

    /* ----- Circle ----- */

    // Not sure what to return
    @Override
    public Circumference getCircumference() {
        return new Circumference();
    }

    @Override
    public void resetCircumference() {

    }

    /* ----- PointSet ----- */

    // Not sure what to return
    @Override
    public Dimension getDimensions() {
        return new Dimension();
    }

    @Override
    public void resetDimentions() {

    }

    /* ----- SineWave ----- */

    @Override
    public Tangent getTangent(Integer source) {
        return new Tangent();
    }

    /* ----- Sequence ----- */

    @Override
    public void setLimit(Integer limit) {

    }

    /* ----- Fruit ----- */

    // Not sure what to return
    @Override
    public Nutrients getNutrients() {
        return new Nutrients();
    }

    @Override
    public void resetNutrients() {

    }

    // Not sure what to return
    @Override
    public Nutrients getAntioxidants() {
        return new Nutrients();
    }

    @Override
    public void resetAntioxidants() {

    }

    /* ----- TabbyCat ----- */

    @Override
    public void purr() {
        System.out.println("Purr");
    }

    /* ----- Lovable ----- */

    public void addAction(String name, Object o) {

    }

    public void canSee(Boolean value) {

    }

    public void addFeeling(String feeling) {

    }

    public void setSatisfaction(Integer level) {

    }

    public Integer toSatisfaction() {
        return 0;
    }

    public Integer getFeelingIndex(String feeling) {
        return 0;
    }

    public void requestExecution(World world) {

    }

    public void toggleGender() {

    }

    public void toggleRoleBDSM() {

    }

    /**
     * Checks if a sense is in the sense list.
     *
     * @param sense The sense to find.
     * @return true if sense is in the list, else false.
     */
    public Boolean getSenseIndex(String sense) {
        return senseList.contains(sense);
    }

    public void removeFeeling(String feeling) {

    }

    public void setOpinion(Object of, Boolean b) {

    }

    public Integer getOpinionIndex(String s) {
        return 0;
    }

    public void setExecution(Object thing) {

    }

    // Not sure what to return
    public Object toExecution() {
        return "";
    }

    public void learnTopic(String topic) {

    }

    public void takeExamTopic(String topic) {

    }

    // Not sure what to return
    public Object getAlgebraicExpression(String s) {
        return "";
    }
}

// ==================== [FILE: Tomato.java] ====================
package goddrinksjava;

public interface Tomato extends Fruit {
}

// ==================== [FILE: World.java] ====================
package goddrinksjava;

import java.util.ArrayList;
import java.util.List;

public class World {
    public final List<Thing> thingList;

    private Boolean settingGod = true;
    private Lovable God;

    public World(Integer i) {
        thingList = new ArrayList<>();
    }

    public void addThing(Thing thing) {
        if (thing instanceof Lovable && settingGod) {
            God = (Lovable) thing;

            settingGod = false;
        }

        thingList.add(thing);
        System.out.println(String.format("Added %s", thing.myName));
    }

    public void startSimulation() {
        System.out.println("Starting simulation");
    }

    public void timeTravelForTwo(String period, Integer year, Thing first, Thing second) {

    }

    public void unite(Thing first, Thing second) {

    }

    public void lockThing(Thing thing) {

    }

    public Lovable getGod() {
        return God;
    }

    public void procreate(Thing... things) {

    }

    public void makeHigh(Thing height) {

    }

    public void unlock(Thing thing) {

    }

    public void removeThing(Thing thing) {
        thingList.remove(thing);
        System.out.println(String.format("Removed %s", thing.myName));
    }

    public void announce(String... s) {

    }

    public void runExecution() {

    }

    public Boolean isExecutableBy(Thing thing) {
        return false;
    }

    public Integer getThingIndex(Thing thing) {
        return 0;
    }

    public void execute(Thing thing) {

    }
}""".trimIndent()

    val CODE_SNIPPETS: List<String> = listOf(
        """public class Antioxidants {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        """public class Attribute {
}""".trimIndent(),
        """public interface Circle extends PointSet {
    // Not sure what to return
    Object getCircumference();

    void resetCircumference();
}""".trimIndent(),
        """public class Circumference {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        """public class Dimension {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        """public interface Eggplant extends Fruit {
}""".trimIndent(),
        """public interface Fruit {
    // Not sure what to return
    Object getNutrients();

    void resetNutrients();""".trimIndent(),
        """    // Not sure what to return
    Object getAntioxidants();

    void resetAntioxidants();
}""".trimIndent(),
        """/*
 * The program GodDrinksJava implements an application that
 * creates an empty simulated world with no meaning or purpose
 *
 * author momocashew""".trimIndent(),
        """ */

public class GodDrinksJava {
    public static void main(String[] args) {
        Thing me = new Lovable("Me", 0, true, -1, false);""".trimIndent(),
        """        Thing you = new Lovable("You", 0, false, -1, false);

        World world = new World(5);
        world.addThing(me);
        world.addThing(you);
        world.startSimulation();""".trimIndent(),
        """        /* --------------- */

        if (me instanceof PointSet) {
            you.addAttribute(me.getDimensions().toAttribute());
            me.resetDimentions();""".trimIndent(),
        """        }

        if (me instanceof Circle) {
            you.addAttribute(me.getCircumference().toAttribute());
            me.resetCircumference();""".trimIndent(),
        """        }

        if (me instanceof SineWave) {
            you.addAction("sit", me.getTangent(you.getXPosition()));
        }""".trimIndent(),
        """        if (me instanceof Sequence) {
            me.setLimit(you.toLimit());
        }

        /* --------------- */""".trimIndent(),
        """        me.toggleCurrent();

        me.canSee(false);
        me.addFeeling("dizzy");

        world.timeTravelForTwo("AC", 617, me, you);""".trimIndent(),
        """        world.timeTravelForTwo("BC", 3691, me, you);

        world.unite(me, you);

        /* --------------- */""".trimIndent(),
        """        if (me.getNumSimulationsAvailable() >= you.getNumSimulationsNeeded()) {
            you.setSatisfaction(me.toSatisfaction());
        }

        if (you.getFeelingIndex("happy") != -1) {""".trimIndent(),
        """            me.requestExecution(world);
        }

        world.lockThing(me);
        world.lockThing(you);""".trimIndent(),
        """        /* --------------- */

        if (me instanceof Eggplant) {
            you.addAttribute(me.getNutrients().toAttribute());
            me.resetNutrients();""".trimIndent(),
        """        }

        if (me instanceof Tomato) {
            you.addAttribute(me.getAntioxidants().toAttribute());
            me.resetAntioxidants();""".trimIndent(),
        """        }

        if (me instanceof TabbyCat) {
            me.purr();
        }""".trimIndent(),
        """        if (world.getGod().equals(me)) {
            me.setProof(you.toProof());
        }

        /* --------------- */""".trimIndent(),
        """        me.toggleGender();
        world.procreate(me, you);
        me.toggleRoleBDSM();

        world.makeHigh(me);""".trimIndent(),
        """        world.makeHigh(you);

        /* --------------- */

        // You think that this function would return the index of the given sense""".trimIndent(),
        """        // But since it's used like this, it might just check if the given sense exists in a list of senses
        if (me.getSenseIndex("vibration")) {
            me.addFeeling("complete");
        }""".trimIndent(),
        """        world.unlock(you);
        world.removeThing(you);

        me.lookFor(you, world);
        me.lookFor(you, world);""".trimIndent(),
        """        me.lookFor(you, world);
        me.lookFor(you, world);
        me.lookFor(you, world);

        if (me.getMemory().isErasable()) {""".trimIndent(),
        """            me.removeFeeling("disheartened");
        }

        try {
            me.setOpinion(me.getOpinionIndex("you are here"), false);""".trimIndent(),
        """        }
        catch (IllegalArgumentException e) {
            world.announce("God is always true");
        }""".trimIndent(),
        """        /* --------------- */

        world.runExecution();
        world.runExecution();
        world.runExecution();""".trimIndent(),
        """        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();""".trimIndent(),
        """        world.runExecution();
        world.runExecution();
        world.runExecution();
        world.runExecution();""".trimIndent(),
        """        world.announce("1", "de");
        world.announce("2", "es");
        world.announce("3", "fr");
        world.announce("4", "kr");
        world.announce("5", "se");""".trimIndent(),
        """        world.announce("6", "cn");

        world.runExecution();

        /* --------------- */""".trimIndent(),
        """        if (world.isExecutableBy(me)) {
            you.setExecution(me.toExecution());
        }

        if (world.getThingIndex(you) != -1) {""".trimIndent(),
        """            world.runExecution();
        }

        me.escape();

        /* --------------- */""".trimIndent(),
        """        me.learnTopic("love");
        me.takeExamTopic("love");
        me.getAlgebraicExpression("love");
        me.escape("love");""".trimIndent(),
        """        /* --------------- */

        world.execute(me);
    }
}""".trimIndent(),
        """public class Lovable extends Thing {
    public Lovable(String name, Integer i, Boolean b, Integer i2, Boolean b2) {
        myName = name;
    }
}""".trimIndent(),
        """public class Memory {
    public Boolean isErasable() {
        return false;
    }
}""".trimIndent(),
        """public class Nutrients {
    public Attribute toAttribute() {
        return new Attribute();
    }
}""".trimIndent(),
        """public interface PointSet {
    // Not sure what to return
    Object getDimensions();

    void resetDimentions();
}""".trimIndent(),
        """public interface Sequence {
    void setLimit(Integer limit);
}""".trimIndent(),
        """public interface SineWave {
    Tangent getTangent(Integer source);
}""".trimIndent(),
        """public interface TabbyCat {
    void purr();
}""".trimIndent(),
        """public class Tangent {
}""".trimIndent(),
        """public class Thing implements Circle, PointSet, SineWave, Sequence, Fruit, TabbyCat {
    public String myName;

    private Memory memory;
    private List<String> senseList;""".trimIndent(),
        """    public Thing() {
        memory = new Memory();
        senseList = new ArrayList<>();
    }""".trimIndent(),
        """    public void addAttribute(Attribute attribute) {

    }

    // Not sure what to return""".trimIndent(),
        """    public Integer getXPosition() {
        return 0;
    }

    // Not sure what to return""".trimIndent(),
        """    public Integer toLimit() {
        return 0;
    }

    public void toggleCurrent() {""".trimIndent(),
        """    }

    public Integer getNumSimulationsAvailable() {
        return 0;
    }""".trimIndent(),
        """    public Integer getNumSimulationsNeeded() {
        return 0;
    }

    public void setProof(Object proof) {""".trimIndent(),
        """    }

    // Not sure what to return
    public Object toProof() {
        return "";
    }""".trimIndent(),
        """    public void lookFor(Thing thing, World world) {
        if (world.thingList.contains(thing)) {
            System.out.println(String.format("Found %s", thing.myName));
        }""".trimIndent(),
        """        else {
            System.out.println(String.format("Could not find %s", thing.myName));
        }
    }""".trimIndent(),
        """    // Not sure what to return
    public Memory getMemory() {
        return memory;
    }

    public void escape(String... things) {""".trimIndent(),
        """    }

    /* ----- Circle ----- */

    // Not sure what to return""".trimIndent(),
        """    @Override
    public Circumference getCircumference() {
        return new Circumference();
    }

    @Override""".trimIndent(),
        """    public void resetCircumference() {

    }

    /* ----- PointSet ----- */""".trimIndent(),
        """    // Not sure what to return
    @Override
    public Dimension getDimensions() {
        return new Dimension();
    }""".trimIndent(),
        """    @Override
    public void resetDimentions() {

    }

    /* ----- SineWave ----- */""".trimIndent(),
        """    @Override
    public Tangent getTangent(Integer source) {
        return new Tangent();
    }

    /* ----- Sequence ----- */""".trimIndent(),
        """    @Override
    public void setLimit(Integer limit) {

    }

    /* ----- Fruit ----- */""".trimIndent(),
        """    // Not sure what to return
    @Override
    public Nutrients getNutrients() {
        return new Nutrients();
    }""".trimIndent(),
        """    @Override
    public void resetNutrients() {

    }

    // Not sure what to return""".trimIndent(),
        """    @Override
    public Nutrients getAntioxidants() {
        return new Nutrients();
    }

    @Override""".trimIndent(),
        """    public void resetAntioxidants() {

    }

    /* ----- TabbyCat ----- */""".trimIndent(),
        """    @Override
    public void purr() {
        System.out.println("Purr");
    }

    /* ----- Lovable ----- */""".trimIndent(),
        """    public void addAction(String name, Object o) {

    }

    public void canSee(Boolean value) {""".trimIndent(),
        """    }

    public void addFeeling(String feeling) {

    }""".trimIndent(),
        """    public void setSatisfaction(Integer level) {

    }

    public Integer toSatisfaction() {
        return 0;
    }""".trimIndent(),
        """    public Integer getFeelingIndex(String feeling) {
        return 0;
    }

    public void requestExecution(World world) {""".trimIndent(),
        """    }

    public void toggleGender() {

    }""".trimIndent(),
        """    public void toggleRoleBDSM() {

    }

    /**""".trimIndent(),
        """     * Checks if a sense is in the sense list.
     *
     * @param sense The sense to find.
     * @return true if sense is in the list, else false.
     */""".trimIndent(),
        """    public Boolean getSenseIndex(String sense) {
        return senseList.contains(sense);
    }

    public void removeFeeling(String feeling) {""".trimIndent(),
        """    }

    public void setOpinion(Object of, Boolean b) {

    }""".trimIndent(),
        """    public Integer getOpinionIndex(String s) {
        return 0;
    }

    public void setExecution(Object thing) {""".trimIndent(),
        """    }

    // Not sure what to return
    public Object toExecution() {
        return "";
    }""".trimIndent(),
        """    public void learnTopic(String topic) {

    }

    public void takeExamTopic(String topic) {""".trimIndent(),
        """    }

    // Not sure what to return
    public Object getAlgebraicExpression(String s) {
        return "";
    }
}""".trimIndent(),
        """public interface Tomato extends Fruit {
}""".trimIndent(),
        """public class World {
    public final List<Thing> thingList;

    private Boolean settingGod = true;
    private Lovable God;""".trimIndent(),
        """    public World(Integer i) {
        thingList = new ArrayList<>();
    }

    public void addThing(Thing thing) {""".trimIndent(),
        """        if (thing instanceof Lovable && settingGod) {
            God = (Lovable) thing;

            settingGod = false;
        }""".trimIndent(),
        """        thingList.add(thing);
        System.out.println(String.format("Added %s", thing.myName));
    }

    public void startSimulation() {""".trimIndent(),
        """        System.out.println("Starting simulation");
    }

    public void timeTravelForTwo(String period, Integer year, Thing first, Thing second) {""".trimIndent(),
        """    }

    public void unite(Thing first, Thing second) {

    }""".trimIndent(),
        """    public void lockThing(Thing thing) {

    }

    public Lovable getGod() {
        return God;
    }""".trimIndent(),
        """    public void procreate(Thing... things) {

    }

    public void makeHigh(Thing height) {""".trimIndent(),
        """    }

    public void unlock(Thing thing) {

    }

    public void removeThing(Thing thing) {""".trimIndent(),
        """        thingList.remove(thing);
        System.out.println(String.format("Removed %s", thing.myName));
    }

    public void announce(String... s) {""".trimIndent(),
        """    }

    public void runExecution() {

    }""".trimIndent(),
        """    public Boolean isExecutableBy(Thing thing) {
        return false;
    }

    public Integer getThingIndex(Thing thing) {
        return 0;
    }""".trimIndent(),
        """    public void execute(Thing thing) {

    }
}""".trimIndent(),
    )

    fun getRandomSnippet(): String {
        if (CODE_SNIPPETS.isEmpty()) return "// [goddrinksjava.Runtime: running]"
        return CODE_SNIPPETS[Random.nextInt(CODE_SNIPPETS.size)]
    }

    fun getCodeForScene(scene: SceneType): String {
        return when (scene) {
            SceneType.BOOT_SEQUENCE,
            SceneType.WORLD_BOOT_INSTRUMENTAL -> ALL_FILES["World.java"] ?: FULL_CODE
            SceneType.GEOMETRY_MATH -> (ALL_FILES["Circle.java"] ?: "") + "\n\n" + (ALL_FILES["PointSet.java"] ?: "") + "\n\n" + (ALL_FILES["SineWave.java"] ?: "")
            SceneType.CURRENT_CONVERSION -> ALL_FILES["Thing.java"] ?: FULL_CODE
            SceneType.SIMULATION_EXECUTION -> (ALL_FILES["Lovable.java"] ?: "") + "\n\n" + (ALL_FILES["World.java"] ?: "")
            SceneType.BIOLOGY_EXISTENCE -> (ALL_FILES["Eggplant.java"] ?: "") + "\n\n" + (ALL_FILES["Tomato.java"] ?: "") + "\n\n" + (ALL_FILES["TabbyCat.java"] ?: "")
            SceneType.GENDER_AND_ROLES -> ALL_FILES["Thing.java"] ?: FULL_CODE
            SceneType.ISOLATION_AND_ERROR -> (ALL_FILES["Memory.java"] ?: "") + "\n\n" + (ALL_FILES["World.java"] ?: "")
            SceneType.ARGUMENT_STACK,
            SceneType.RAPID_EXECUTION_STORM,
            SceneType.DESPERATE_EXECUTION -> ALL_FILES["World.java"] ?: FULL_CODE
            SceneType.ALGEBRAIC_LOVE -> ALL_FILES["Lovable.java"] ?: FULL_CODE
            SceneType.OPEN_LOOP_SHUTDOWN -> ALL_FILES["GodDrinksJava.java"] ?: FULL_CODE
        }
    }
}