package com.quiptiq.ludumdare

import com.badlogic.ashley.core.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector3
import com.quiptiq.ludumdare.ld35.entity.*
import com.quiptiq.ludumdare.ld35.game.Level
import java.util.*

class LogicHandler {
    companion object {
        private val NUM_CREATURES: Int = 5;
        private val creatures: Family =
                Family.all(Position::class.java, Movement::class.java)
                        .exclude(Mouseable::class.java)
                        .get();
        private val preyFamily: Family =
                Family.all(Prey::class.java).exclude(Mouseable::class.java).get()
        private val obstacleFamily: Family =
                Family.all(FixedObjectRepulsor::class.java).get()
        private val POSN_MAPPER: ComponentMapper<Position> =
                ComponentMapper.getFor(Position::class.java)
        private val MVMNT_MAPPER: ComponentMapper<Movement> =
                ComponentMapper.getFor(Movement::class.java)
        private val MODEL_MAPPER: ComponentMapper<ModelComponent> =
                ComponentMapper.getFor(ModelComponent::class.java)
        private val CROWDING_MAPPER: ComponentMapper<CrowdingRepulsor> =
                ComponentMapper.getFor(CrowdingRepulsor::class.java)
        private val COHESION_MAPPER: ComponentMapper<CohesionAttractor> =
                ComponentMapper.getFor(CohesionAttractor::class.java)
        private val PREDATOR_PREY_MAPPER: ComponentMapper<PredatorPreyRepulsor> =
                ComponentMapper.getFor(PredatorPreyRepulsor::class.java)
        private val PREY_PREDATOR_MAPPER: ComponentMapper<PreyPredatorAttractor> =
                ComponentMapper . getFor (PreyPredatorAttractor::class.java)
        // Lists what the entity is attracted to
        private val ATTRACTED_BY_MAPPER: ComponentMapper<AttractedByLister> =
                ComponentMapper.getFor(AttractedByLister::class.java)
        private val PREY_MAPPER: ComponentMapper<Prey> =
                ComponentMapper.getFor(Prey::class.java)
        private val PREDATOR_MAPPER = ComponentMapper.getFor(Predator::class.java)
        private val HIDDEN_PREDATOR_MAPPER = ComponentMapper.getFor(PredatorHidden::class.java)
        private val OBSTACLE_MAPPER = ComponentMapper.getFor(FixedObjectRepulsor::class.java)
        // Distance to player within which evasion is attempted (^ 2)
        private val PLAYER_EFFECT_RANGE = (20 * 20).toFloat()
        // Speed at which evasion is attempted, units/sec
        private val EVASION_SPEED = 30f
        // Speed at which chasing is attempted, units/sec
        private val CHASE_SPEED = 30f
        private val MAX_SPEED = 120f
        // Default height above the "ground" at which to draw entities
        private val DEFAULT_HEIGHT = 0f
        private val DEFAULT_ROTATION = ImmutableVector3(0, 0, 1)
        private val UP: ImmutableVector3 = ImmutableVector3(0, 1, 0);

        // Differentiate player
        class Mouseable : Component

        // Simple reference to the model
        data class ModelComponent(val model: IntersectableModel) : Component

        /**
         * Max speed at which prey is attracted to/repulsed by things
         */
        private val PREY_ATTRACTOR_SPEEDS: EnumMap<AttractorType, Float>

        init {
            val speeds: EnumMap<AttractorType, Float> =
                EnumMap<AttractorType, Float>(AttractorType::class.java)
            speeds.put(AttractorType.CROWDING, 30f);
            speeds.put(AttractorType.COHESION, 3f);
            speeds.put(AttractorType.PLAYER, EVASION_SPEED);
            speeds.put(AttractorType.PREY_PREDATOR, EVASION_SPEED);
            speeds.put(AttractorType.HIDDEN_PREDATOR, 30f);
            speeds.put(AttractorType.PREDATOR_PREY, CHASE_SPEED);
            for (type : AttractorType.values()) {
                if (speeds.get(type) == null) {
                    speeds.put(type, 3f);
                }
            }
            PREY_ATTRACTOR_SPEEDS = speeds;
        }

        class PredatorHidden : Component

        class Predator : Component

        interface ModelInstanceProvider {
            fun createModel(e: Entity, position: Vector3): IntersectableModel
        }

        private interface AttractorProvider {
            fun getAttractorsFoundIn(entity: Entity): List<Attractor>
        }

        /**
         * When attached to an entity, lists all attractors on another entity in
         * which this one is interested.
         */
        private class AttractedByLister(private val provider: AttractorProvider) : Component {

            /**
             * Lists all attractors on the specified entity in which this entity
             * is interested
             * @param entity Entity on which interesting attractors are listed
             * @return List of attractors
             */
            fun getAttractorsFoundIn(entity: Entity): List<Attractor> {
                return provider.getAttractorsFoundIn(entity);
            }
        }
    }

    private val movementCalculator: MovementCalculator = MovementCalculator(DEFAULT_ROTATION);

    /**
     * Provides all attractors in which a prey is interested
     */
    private val PREY_ATTRACTORS_LISTER: AttractedByLister = AttractedByLister(
    AttractorProvider() {
        override fun getAttractorsFoundIn(entity: Entity): List<Attractor> {
            List<Attractor> attractors = new ArrayList<Attractor>();
            for (Attractor attractor : Attractor[]{
            CROWDING_MAPPER.get(entity),
            COHESION_MAPPER.get(entity),
            PREDATOR_PREY_MAPPER.get(entity)
        }) {
            if (attractor != null) {
                attractors.add(attractor);
            }
        }
            return attractors;
        }
    });

    /**
     * Provides all attractors in which a predator is interested
     */
    private val PREDATOR_ATTRACTORS_LISTER: AttractedByLister = AttractedByLister(
    AttractorProvider() {
        override fun getAttractorsFoundIn(entity: Entity): List<Attractor> {
            val attractors: List<Attractor> = ArrayList<Attractor>();
            val prey: Prey = PREY_MAPPER.get(entity);
            if (prey != null && !prey.isDead()) {
                val attractor: PreyPredatorAttractor = PREY_PREDATOR_MAPPER.get(entity);
                if (attractor != null) {
                    attractors.add(attractor);
                }
            }
            return attractors;
        }
    });

    private val projectionTranslator: ProjectionTranslator

    private val staleModelEntities: ArrayList<Entity> = ArrayList<Entity>();

    /**
     * @return All entities with stale models
     */
    fun getStaleModelEntities(): List<Entity> {
        return staleModelEntities;
    }

    private fun getDelta(): Float {
        return Gdx.graphics.getDeltaTime();
    }

    /**
     * Creates a new creature basic position and movement attributes
     * @param position Creature position
     * @param rotation Creature rotation
     * @return Newly created creature
     */
    fun createNewCreature(position: Vector3, rotation: Vector3): Entity {
        val creature = Entity();
        if (rotation.isZero()) {
            creature.add(Position(position, DEFAULT_ROTATION.copy()));
        }
        creature.add(Position(position, rotation));
        creature.add(Movement(Vector3(), Vector3(0f, 0f, 0f)));
        return creature;
    }

    /**
     * Creates a new prey creature, assigned with all prey-related components
     * @param position Initial position
     * @param rotation Initial rotation
     * @return Prey entity
     */
    fun createNewPrey(position: Vector3, rotation: Vector3): Entity {
        val creature = createNewCreature(position, rotation);
        creature.add(CrowdingRepulsor())
                .add(CohesionAttractor())
                .add(PreyPredatorAttractor())
                .add(Prey())
                .add(PREY_ATTRACTORS_LISTER);
        return creature;
    }

    /**
     * Creates a new predator creature, assigned with all predator-related components
     * @param position Initial position
     * @param rotation Initial rotation
     * @return Predator entity
     */
    fun createNewPredator(position: Vector3, rotation: Vector3): Entity {
        val creature = createNewCreature(position, rotation);
        creature.add(PredatorPreyRepulsor())
                .add(Predator())
                .add(PREDATOR_ATTRACTORS_LISTER);
        return creature;
    }

    private fun getCurrentLevel(): Level {
        return GraphicsHandler.getGraphicsHandler().getWorldRenderer().getCurrentLevel();
    }

    fun getRotationAngleTowards(from: Vector3, to: Vector3): Float {
        var dir = Math.atan2((from.x - to.x).toDouble(), (from.z - to.z).toDouble());
        dir = dir * (180 / Math.PI);

        return dir.toFloat()
    }

    fun update() {
        staleModelEntities.clear();
        val engine = getCurrentLevel().getEngine();
        val player = getCurrentLevel().getPlayer();

        // Move the player towards the 3d position the mouse is pointing to?
        val worldMousePosn: Vector3 = projectionTranslator.unproject(Gdx.input.getX(), Gdx.input.getY());
        val position: Vector3 = POSN_MAPPER.get(player).position;
        val velocity: Vector3 = MVMNT_MAPPER.get(player).velocity;
        velocity.set(worldMousePosn).sub(position);

        val collisionModels: Array<IntersectableModel> = getCurrentLevel().getCollisionModels();
        val model: IntersectableModel = MODEL_MAPPER.get(player).model;

        // 90f because the model is wrong
        model.transform.setToRotation(Vector3.Y, getRotationAngleTowards(position, worldMousePosn) + 90f);

        if (!collides(player, collisionModels)) {
            position.set(worldMousePosn);
            model.updateCollisions();
        }
        ImmutableArray<Entity> entities = engine.getEntitiesFor(creatures);
        calculateMovementChanges(engine, player, entities);
        applyMovementChanges(entities);
    }

    /**
     * Calculate the changes in the flock's movement. Note current algorithm is
     * O(n^2) due to the need to look at all other members of the flock.
     * Partitioning by location would improve this.
     */
    private void calculateMovementChanges(Engine engine, Entity player, ImmutableArray<Entity> entities) {

        Position playerPosn = POSN_MAPPER.get(player);

        // First calculate change in position for all objects.
        // Note that re-used of entity collection iterators means you can't
        // have inner loops, hence use of indices.
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            Position posn = POSN_MAPPER.get(entity);
            Movement movement = MVMNT_MAPPER.get(entity);
            Vector3 change;

            Predator predator = entity.getComponent(Predator.class);
            // Attempt to move away from the player
            float dist2ToPlayer = posn.position.dst2(playerPosn.position);
            if (dist2ToPlayer <= PLAYER_EFFECT_RANGE) {
                float speed = 60 * PLAYER_EFFECT_RANGE / (dist2ToPlayer * dist2ToPlayer);
                change = movementCalculator.awayFrom(posn, playerPosn).nor().scl(speed).clamp(0, EVASION_SPEED);
                if (predator != null) {
                    // Moving away from the player overrides all else
                    prepareMovement(movement, change.scl(getDelta()));
                    continue;
                }
            } else {
                change = new Vector3();
            }

            // Try to avoid crowding neighbors, yet keep in distance
            ImmutableArray<Entity> others = engine.getEntitiesFor(creatures);
            // Find out what this entity is attracted to
            EnumMap<AttractorType, Vector3> attractors =
            new EnumMap<AttractorType, Vector3>(AttractorType.class);
            for (AttractorType attractorType : AttractorType.values()) {
            attractors.put(attractorType, new Vector3());
        }
            // Get a lister of attractors in which this entity is interested
            AttractedByLister lister = ATTRACTED_BY_MAPPER.get(entity);
            for (int j = 0; j < others.size(); j++) {
            Entity other = others.get(j);
            if (entity == other) {
                continue;
            }
            Position otherPosn = POSN_MAPPER.get(other);
            float distance = posn.position.dst2(otherPosn.position);
            // List all interesting attractors on the other entity
            for (Attractor attractor : lister.getAttractorsFoundIn(other)) {

            if (distance <= attractor.getMaxRange()) {
                float speed = attractor.getBaseSpeed(distance);
                Vector3 direction = movementCalculator.towards(posn, otherPosn).nor();
                attractors.get(attractor.getAttractorType()).add(direction.scl(speed));
            }
        }
        }
            // Add fixed attractors
            ImmutableArray<Entity> obstacles = engine.getEntitiesFor(obstacleFamily);
            Vector3 obstacleChange = attractors.get(AttractorType.FIXED_OBSTACLE);
            for (int obstacleIdx = 0; obstacleIdx < obstacles.size(); obstacleIdx++) {
            Entity obstacle = obstacles.get(obstacleIdx);
            FixedObjectRepulsor repulsor = OBSTACLE_MAPPER.get(obstacle);
            Position otherPosn = POSN_MAPPER.get(obstacle);
            if (repulsor == null || otherPosn == null) {
                System.out.println("Null found for obstacle " + repulsor + ", " + otherPosn);
                continue;
            }
            float distance = posn.position.dst2(otherPosn.position);
            if (distance <= repulsor.getMaxRange()) {
                float speed = repulsor.getBaseSpeed(distance);
                Vector3 direction = movementCalculator.towards(posn, otherPosn).nor();
                obstacleChange.add(direction.scl(speed));
            }
        }
            // Add all attractions together, clamped by their speed
            for (AttractorType attractorType : AttractorType.values()) {
            Vector3 attractantChange = attractors.get(attractorType);
            float speed = PREY_ATTRACTOR_SPEEDS.get(attractorType);
            change.add(attractantChange.clamp(0, speed));
        }
            // speed is per second, so scale according to seconds
            prepareMovement(movement, change.scl(getDelta()));
        }
    }

    /**
     * Prepares movement updates based on change
     * @param movement Movement to be updated
     * @param change Changes to movement
     */
    public void prepareMovement(Movement movement, Vector3 change) {
        change.clamp(0, MAX_SPEED);
        // any y values for now are set to default height
        change.y = DEFAULT_HEIGHT;
        // velocity changes to the average between the desired and current. Does this work? Idk
        movement.velocity.add(change).scl(0.5f);
        movement.rotation.set(change).nor();
    }

    private Vector2 createXZVector(Vector3 vector3) {
        return new Vector2(vector3.x, vector3.z);
    }

    private boolean collides(Entity entity, Array<IntersectableModel> boxes) {
        Position posn = POSN_MAPPER.get(entity);
        Movement movement = MVMNT_MAPPER.get(entity);
        IntersectableModel model = MODEL_MAPPER.get(entity).model;
        Vector2 start = createXZVector(posn.position);
        Vector2 end = new Vector2(start).add(createXZVector(movement.velocity));
        for (IntersectableModel boxModel : boxes) {
            if (boxModel == model) {
                continue;
            }

            if (boxModel.lineIntersectsCollision(start, end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies movement changes to positions IFF the entity is not moving into
     * the bounding box of another.
     * @param entities All entities to be moved
     */
    public void applyMovementChanges(ImmutableArray<Entity> entities) {
        // now apply the changes to each position
        Vector3 up = UP.copy();
        float buffer = 0.01f;
        Engine engine = getCurrentLevel().getEngine();

        Array<IntersectableModel> boxes = getCurrentLevel().getCollisionModels();
        for (Entity entity : entities) {
            Position posn = POSN_MAPPER.get(entity);
            Movement movement = MVMNT_MAPPER.get(entity);
            IntersectableModel model = MODEL_MAPPER.get(entity).model;
            // Don't change position if it results in a collision
            if (collides(entity, boxes)) {
                continue;
            }

            Vector3 originalPosition = new Vector3(posn.position);
            posn.position.add(movement.velocity);
            // 90f because the models are wrong
            model.transform.setToRotation(Vector3.Y,
                    getRotationAngleTowards(originalPosition, posn.position) + 90f);
            model.transform.setTranslation(posn.position);
            model.updateCollisions();
            Prey prey = PREY_MAPPER.get(entity);
            if (PREY_MAPPER.get(entity) != null) {
                if (!prey.isDead()) {
                    prey.setPenned(getCurrentLevel().getSheepPenModel().contains(model));
                    if (prey.isPenned()) {
                        // no longer attracts those outside
                        entity.remove(CohesionAttractor.class);
                        entity.remove(PreyPredatorAttractor.class);
                        entity.remove(AttractedByLister.class);
                        entity.remove(PredatorHidden.class);
                        entity.add(new AttractedByLister(new AttractorProvider() {
                            @Override
                            public List<Attractor> getAttractorsFoundIn(Entity entity) {
                                List<Attractor> attractors = new ArrayList<Attractor>();
                                CrowdingRepulsor crowding = CROWDING_MAPPER.get(entity);
                                if (crowding != null) {
                                    attractors.add(crowding);
                                }
                                return attractors;
                            }
                        }));
                    }
                    if (!prey.isPenned() && HIDDEN_PREDATOR_MAPPER.get(entity) != null) {
                        for (IntersectableModel transformer : getCurrentLevel().getWolfTransformModels()) {
                            if (transformer.contains(model)) {
                                // Remove all prey components and add predator
                                entity.remove(Prey.class);
                                entity.remove(CrowdingRepulsor.class);
                                entity.remove(CohesionAttractor.class);
                                entity.remove(PreyPredatorAttractor.class);
                                entity.remove(AttractedByLister.class);
                                entity.remove(PredatorHidden.class);

                                entity.add(new PredatorPreyRepulsor())
                                        .add(new Predator())
                                        .add(PREDATOR_ATTRACTORS_LISTER);
                                staleModelEntities.add(entity);
                                break;
                            }
                        }
                    }

                    if (prey.isPenned()) {
                        model.materials.get(1).set(ColorAttribute.createDiffuse(Color.GREEN));
                    } else {
                        model.materials.get(1).set(ColorAttribute.createDiffuse(Color.WHITE));
                    }
                }
            }
            Predator predator = PREDATOR_MAPPER.get(entity);
            if (predator != null) {
                // for each prey, if this predator intersects that prey, kill it.
                ImmutableArray<Entity> potentialPreys = engine.getEntitiesFor(preyFamily);
                for (int i = 0; i < potentialPreys.size(); i++) {
                    Entity potentialPrey = potentialPreys.get(i);
                    prey = PREY_MAPPER.get(potentialPreys.get(i));
                    if (prey == null) {
                        continue;
                    }
                    IntersectableModel preyModel = MODEL_MAPPER.get(potentialPrey).model;
                    if (preyModel.intersects(model)) {
                        if (!prey.isDead()) {
                            preyModel.materials.get(1).set(ColorAttribute.createDiffuse(Color.RED));
                            preyModel.transform.translate(0f, 1f, 0f).rotate(Vector3.X, 90).translate(0f, -1f, 0f);
                            GraphicsHandler.getGraphicsHandler().getWorldRenderer().getAnimController(preyModel).paused = true;

                            if (!GraphicsHandler.getGraphicsHandler().getWorldRenderer().mute) {
                                GraphicsHandler.getGraphicsHandler().getWorldRenderer().wolfHowl.play(0.75f);
                                GraphicsHandler.getGraphicsHandler().getWorldRenderer().killSound.play(0.25f);
                            }
                        }
                        prey.kill();
                        // dead prey can't move
                        potentialPrey.remove(Movement.class);
                        // dead prey has no flock or predator attractors
                        potentialPrey.remove(CrowdingRepulsor.class);
                        potentialPrey.remove(CohesionAttractor.class);
                        potentialPrey.remove(PreyPredatorAttractor.class);
                    }
                }
            }
        }
    }

    public int getNumPenned() {
        ImmutableArray<Entity> entities = getCurrentLevel().getEngine().getEntitiesFor(preyFamily);
        int count = 0;
        for (Entity entity : entities) {
            Prey prey = PREY_MAPPER.get(entity);
            if (!prey.isDead() && prey.isPenned()) {
                count++;
            }
        }
        return count;
    }

    public int getNumDead() {
        ImmutableArray<Entity> entities = getCurrentLevel().getEngine().getEntitiesFor(preyFamily);
        int count = 0;
        for (Entity entity : entities) {
            Prey prey = PREY_MAPPER.get(entity);
            if (prey.isDead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 	Creates models for each of the creatures, using the callback and assigning models to each creature
     */
    public void createCreatures(ModelInstanceProvider modelProvider) {
        Engine engine = GraphicsHandler.getGraphicsHandler().getWorldRenderer().getCurrentLevel().getEngine();

        for (Entity entity : engine.getEntitiesFor(creatures)) {
            IntersectableModel model = modelProvider.createModel(entity, POSN_MAPPER.get(entity).position);
            entity.add(new ModelComponent(model));
        }
    }

    public void setProjectionTranslator(ProjectionTranslator translator) {
        this.projectionTranslator = translator;
    }

    public Vector3 getPlayerPosn() {
        Entity player = GraphicsHandler.getGraphicsHandler().getWorldRenderer().getCurrentLevel().getPlayer();

        return new Vector3(POSN_MAPPER.get(player).position);
    }
}
