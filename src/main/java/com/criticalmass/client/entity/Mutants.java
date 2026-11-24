package com.criticalmass.client.entity;

import com.criticalmass.CriticalMass;
import com.criticalmass.content.radiation.IrradiatedCreeper;
import com.criticalmass.content.radiation.Irradiation;
import com.criticalmass.mixin.AgeableListModelAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;

public final class Mutants {
    private static final ResourceLocation GROWTHS = CriticalMass.asResource("textures/entity/mutation_growths.png");
    private static final ResourceLocation GROWTHS_GLOW = CriticalMass.asResource("textures/entity/mutation_growths_glow.png");
    private static final Map<EntityModel<?>, Rig> RIGS = new WeakHashMap<>();
    private static final Map<EntityType<?>, Species> SPECIES = new IdentityHashMap<>();
    private static final ArrayDeque<Bend> BENDS = new ArrayDeque<>();
    private static final PoseStack SCRATCH = new PoseStack();
    private static final ModelPart BOIL = new Build().pus(-1, -2, -1, 2, 2, 2).bake();
    private static final ModelPart BIG_BOIL = new Build().pus(-1.5F, -2, -1.5F, 3, 3, 3).bake();
    private static final ModelPart SPUR = new Build().bone(-0.5F, -4, -0.5F, 1, 4, 1).bake();

    private Mutants() {
    }

    record Rig(@Nullable ModelPart root, List<ModelPart> head, List<ModelPart> body, List<ModelPart> limbs) {
        @Nullable
        ModelPart torso() {
            return !body.isEmpty() ? body.get(0) : limbs.isEmpty() ? null : limbs.get(0);
        }
    }

    record Saved(ModelPart part, PartPose pose, float x, float y, float z) {
    }

    interface Shape {
        void apply(Bend bend, Rig rig, EntityModel<?> model);
    }

    interface Host {
        @Nullable
        ModelPart of(Rig rig);
    }

    record Graft(Host host, ModelPart part) {
    }

    record Twin(Host source, float x, float y, float z, float xRot, float yRot, float zRot, float swing, float scale) {
    }

    record Species(@Nullable Shape shape, List<Graft> grafts, List<Twin> twins) {
    }

    static final class Build {
        private final CubeListBuilder cubes = CubeListBuilder.create();

        private Build box(int u, int v, float x, float y, float z, float w, float h, float d) {
            cubes.texOffs(u, v).addBox(x, y, z, w, h, d);
            return this;
        }

        Build bone(float x, float y, float z, float w, float h, float d) {
            return box(0, 0, x, y, z, w, h, d);
        }

        Build pus(float x, float y, float z, float w, float h, float d) {
            return box(32, 0, x, y, z, w, h, d);
        }

        Build hide(float x, float y, float z, float w, float h, float d) {
            return box(0, 32, x, y, z, w, h, d);
        }

        ModelPart bake() {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("part", cubes, PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("part");
        }
    }

    static final class Bend {
        private final EntityModel<?> model;
        private final List<Saved> saved = new ArrayList<>();

        Bend(EntityModel<?> model) {
            this.model = model;
        }

        private boolean keep(@Nullable ModelPart part) {
            if (part == null) {
                return false;
            }
            for (Saved s : saved) {
                if (s.part() == part) {
                    return true;
                }
            }
            saved.add(new Saved(part, part.storePose(), part.xScale, part.yScale, part.zScale));
            return true;
        }

        void scale(@Nullable ModelPart part, float x, float y, float z) {
            if (keep(part)) {
                part.xScale *= x;
                part.yScale *= y;
                part.zScale *= z;
            }
        }

        void turn(@Nullable ModelPart part, float x, float y, float z) {
            if (keep(part)) {
                part.xRot += x;
                part.yRot += y;
                part.zRot += z;
            }
        }

        void move(@Nullable ModelPart part, float x, float y, float z) {
            if (keep(part)) {
                part.x += x;
                part.y += y;
                part.z += z;
            }
        }

        void undo() {
            for (int i = saved.size() - 1; i >= 0; i--) {
                Saved s = saved.get(i);
                s.part().loadPose(s.pose());
                s.part().xScale = s.x();
                s.part().yScale = s.y();
                s.part().zScale = s.z();
            }
        }
    }

    private static Host head(int index) {
        return rig -> index < rig.head().size() ? rig.head().get(index) : null;
    }

    private static Host body(int index) {
        return rig -> index < rig.body().size() ? rig.body().get(index) : null;
    }

    private static Host named(String name) {
        return rig -> rig.root() != null && rig.root().hasChild(name) ? rig.root().getChild(name) : null;
    }

    private static void species(@Nullable Shape shape, List<Graft> grafts, List<Twin> twins, EntityType<?>... types) {
        for (EntityType<?> type : types) {
            SPECIES.put(type, new Species(shape, grafts, twins));
        }
    }

    static {
        species((bend, rig, model) -> {
            bend.scale(head(0).of(rig), 1.15F, 1.15F, 1.15F);
            bend.scale(body(0).of(rig), 1.14F, 1.05F, 1.12F);
            bend.scale(body(3).of(rig), 1.3F, 1F, 1.3F);
            bend.scale(body(4).of(rig), 1.3F, 1F, 1.3F);
        }, List.of(
                new Graft(head(0), new Build().bone(-5, 1.5F, -10, 2, 1.5F, 2).bone(-6, -1.5F, -10, 1.5F, 3.5F, 1.5F).bone(-6, -3, -9.75F, 1, 1.5F, 1)
                        .bone(3, 1.5F, -10, 2, 1.5F, 2).bone(4.5F, -1.5F, -10, 1.5F, 3.5F, 1.5F).bone(5, -3, -9.75F, 1, 1.5F, 1).bake()),
                new Graft(body(0), new Build().hide(-1, -9, 1, 2, 4, 3).hide(-1, -5, 1, 2, 4, 2).hide(-1, -1, 1, 2, 5, 1).bake())),
                List.of(), EntityType.PIG);
        species((bend, rig, model) -> {
            bend.scale(head(0).of(rig), 1.1F, 1.1F, 1.1F);
            bend.scale(body(0).of(rig), 1.15F, 1F, 1.1F);
            bend.scale(body(3).of(rig), 1.25F, 1F, 1.25F);
            bend.scale(body(4).of(rig), 1.25F, 1F, 1.25F);
        }, List.of(
                new Graft(head(0), new Build().bone(-8, -4, -5, 4, 2, 2).bone(-9, -8, -5, 2, 4, 2).bone(-9, -10, -5, 1, 2, 1)
                        .bone(4, -4, -5, 4, 2, 2).bone(7, -8, -5, 2, 4, 2).bone(8, -10, -5, 1, 2, 1).bake()),
                new Graft(body(0), new Build().hide(-4, -9, 3, 8, 7, 3).pus(1, -6, 6, 2, 2, 1).bake())),
                List.of(), EntityType.COW, EntityType.MOOSHROOM);
        species((bend, rig, model) -> {
            bend.scale(head(0).of(rig), 1.12F, 1.12F, 1.12F);
            bend.scale(body(2).of(rig), 0.7F, 1F, 0.7F);
            bend.scale(body(3).of(rig), 1.3F, 1F, 1.3F);
        }, List.of(new Graft(head(0), new Build().bone(-4.5F, -4, -2, 1.5F, 2, 3).bone(-5, -2.5F, -4.5F, 1.5F, 2.5F, 3).bone(-4.5F, 0, -5.5F, 1, 1.5F, 2)
                .bone(3, -4, -2, 1.5F, 2, 3).bone(3.5F, -2.5F, -4.5F, 1.5F, 2.5F, 3).bone(3.5F, 0, -5.5F, 1, 1.5F, 2).bake())),
                List.of(), EntityType.SHEEP);
        species((bend, rig, model) -> {
            bend.scale(body(0).of(rig), 1.2F, 1.1F, 1.2F);
            bend.turn(head(0).of(rig), 0, 0, -0.25F);
            bend.turn(head(1).of(rig), 0, 0, -0.25F);
            bend.turn(head(2).of(rig), 0, 0, -0.25F);
        }, List.of(new Graft(body(0), new Build().pus(-2, -1, 3, 2, 2, 1).bone(1, -3, 3, 1, 1, 3).bake())),
                List.of(new Twin(head(0), 2.5F, 0.5F, 0, 0, 0, 0.75F, 1F, 0.85F), new Twin(head(1), 2.5F, 0.5F, 0, 0, 0, 0.75F, 1F, 0.85F), new Twin(head(2), 2.5F, 0.5F, 0, 0, 0, 0.75F, 1F, 0.85F)),
                EntityType.CHICKEN);
        species((bend, rig, model) -> {
            bend.scale(head(0).of(rig), 1.25F, 1.25F, 1.25F);
            bend.scale(body(6).of(rig), 1.2F, 1.1F, 1.2F);
            bend.scale(body(3).of(rig), 1.4F, 1F, 1.4F);
        }, List.of(
                new Graft(head(0), new Build().bone(-0.5F, 3, -5, 1, 3, 1).bone(1.5F, 3, -5, 1, 3, 1).bake()),
                new Graft(body(0), new Build().hide(-1, -1, 3, 2, 3, 2).hide(-1, 3, 3, 2, 3, 1).bake()),
                new Graft(body(6), new Build().hide(0, -2, 4, 2, 4, 3).bake())),
                List.of(), EntityType.WOLF);
        species((bend, rig, model) -> {
            if (model instanceof HumanoidModel<?> human) {
                bend.scale(human.body, 1.6F, 1.05F, 1.5F);
                bend.scale(human.rightArm, 2F, 1F, 2F);
                bend.scale(human.leftArm, 2F, 1F, 2F);
                bend.move(human.rightArm, -2.6F, 0, 0);
                bend.move(human.leftArm, 2.6F, 0, 0);
                bend.scale(human.rightLeg, 1.8F, 1F, 1.8F);
                bend.scale(human.leftLeg, 1.8F, 1F, 1.8F);
                bend.move(human.rightLeg, -0.9F, 0, 0);
                bend.move(human.leftLeg, 0.9F, 0, 0);
                bend.scale(human.head, 1.12F, 1.12F, 1.12F);
                bend.scale(human.hat, 1.12F, 1.12F, 1.12F);
            }
        }, List.of(new Graft(body(0), new Build().bone(-0.5F, 1, 2, 1, 1, 3).bone(-0.5F, 4, 2, 1, 1, 4).bone(-0.5F, 7, 2, 1, 1, 3).pus(-3, 2, -3, 2, 2, 1).bake())),
                List.of(new Twin(body(1), -0.5F, 8, 0, -0.3F, 0, 0.32F, -0.6F, 0.85F), new Twin(body(2), 0.5F, 8, 0, -0.3F, 0, -0.32F, -0.6F, 0.85F)),
                EntityType.ENDERMAN);
        species((bend, rig, model) -> {
            if (model instanceof HumanoidModel<?> human) {
                bend.scale(human.rightArm, 1.7F, 1.15F, 1.7F);
                bend.scale(human.leftArm, 0.7F, 1F, 0.7F);
                bend.turn(human.head, 0, 0, 0.22F);
                bend.turn(human.hat, 0, 0, 0.22F);
                bend.scale(human.body, 1.1F, 1F, 1.15F);
            }
        }, List.of(
                new Graft(body(1), new Build().bone(-2.5F, -5, -0.5F, 1, 3, 1).bone(-1, -6, 0.5F, 1, 4, 1).bone(-0.5F, -4, -1.5F, 1, 2, 1).pus(-4, 4, -1, 1, 2, 2).bake()),
                new Graft(body(0), new Build().hide(-3, 1, 2, 6, 5, 2).pus(1, 2, 4, 2, 2, 1).bake())),
                List.of(), EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER);
        species((bend, rig, model) -> {
            if (model instanceof HumanoidModel<?> human) {
                bend.scale(human.rightArm, 1F, 1.25F, 1F);
                bend.scale(human.leftArm, 1F, 1.25F, 1F);
                bend.turn(human.head, 0, 0, -0.2F);
                bend.turn(human.hat, 0, 0, -0.2F);
            }
        }, List.of(new Graft(body(0), new Build().bone(-0.5F, 1, 2, 1, 1, 3).bone(-0.5F, 4, 2, 1, 1, 4).bone(-0.5F, 7, 2, 1, 1, 3).bake())),
                List.of(new Twin(head(0), 5.5F, 1.5F, 0, 0, 0, 0.75F, 1F, 0.72F)),
                EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON);
        species((bend, rig, model) -> {
            bend.scale(named("body1").of(rig), 1.45F, 1.45F, 1.4F);
            bend.scale(named("head").of(rig), 1.15F, 1.15F, 1.15F);
        }, List.of(new Graft(named("body1"), new Build().pus(-4, -7, -4, 3, 3, 3).pus(1, -6, 0, 3, 2, 3).pus(-2, -6, 3, 2, 2, 2).bone(-0.5F, -7, -5, 1, 3, 1).bone(3, -7, 4, 1, 3, 1).bake())),
                List.of(), EntityType.SPIDER, EntityType.CAVE_SPIDER);
        species((bend, rig, model) -> {
            bend.scale(named("head").of(rig), 1.3F, 1.25F, 1.3F);
            bend.turn(named("head").of(rig), 0, 0, 0.18F);
            bend.scale(named("right_leg").of(rig), 1.35F, 1F, 1.35F);
        }, List.of(
                new Graft(named("head"), new Build().pus(1, -3, -6, 3, 3, 2).bone(-3, -13, 1, 1, 3, 1).bone(2, -12, -2, 1, 2, 1).bake()),
                new Graft(named("body"), new Build().hide(-3, 0, 3, 6, 5, 3).pus(-2, 2, 6, 2, 2, 1).bake())),
                List.of(), EntityType.VILLAGER, EntityType.WANDERING_TRADER);
    }

    public static boolean shows(LivingEntity entity) {
        return entity instanceof Mob mob && !(mob instanceof IrradiatedCreeper) && Irradiation.flagged(mob);
    }

    public static ResourceLocation skin(LivingEntity entity, ResourceLocation original) {
        return shows(entity) ? MutantSkins.of(original).colour() : original;
    }

    private static List<ModelPart> list(Iterable<ModelPart> parts) {
        List<ModelPart> out = new ArrayList<>();
        parts.forEach(out::add);
        return out;
    }

    static Rig rig(EntityModel<?> model) {
        Rig rig = RIGS.get(model);
        if (rig == null) {
            rig = read(model);
            RIGS.put(model, rig);
        }
        return rig;
    }

    private static Rig read(EntityModel<?> model) {
        if (model instanceof HumanoidModel<?> human) {
            return new Rig(null, List.of(human.head), List.of(human.body, human.rightArm, human.leftArm, human.rightLeg, human.leftLeg), List.of(human.rightArm, human.leftArm, human.rightLeg, human.leftLeg));
        }
        if (model instanceof AgeableListModel<?>) {
            AgeableListModelAccessor parts = (AgeableListModelAccessor) model;
            List<ModelPart> body = list(parts.criticalmass$bodyParts());
            return new Rig(null, list(parts.criticalmass$headParts()), body, body.size() > 1 ? body.subList(1, body.size()) : List.of());
        }
        if (model instanceof HierarchicalModel<?> tree) {
            ModelPart root = tree.root();
            List<ModelPart> head = root.hasChild("head") ? List.of(root.getChild("head")) : List.of();
            List<ModelPart> body = root.hasChild("body") ? List.of(root.getChild("body")) : List.of();
            List<ModelPart> limbs = root.getAllParts().filter(part -> part != root && !part.isEmpty() && !head.contains(part) && !body.contains(part)).toList();
            return new Rig(root, head, body, limbs);
        }
        if (model instanceof ListModel<?> listed) {
            return new Rig(null, List.of(), List.of(), list(listed.parts()));
        }
        return new Rig(null, List.of(), List.of(), List.of());
    }

    private static float chance(long seed, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1 << 24);
    }

    public static void deform(LivingEntity entity, EntityModel<?> model) {
        if (!shows(entity)) {
            return;
        }
        Rig rig = rig(model);
        Bend bend = new Bend(model);
        BENDS.push(bend);
        Species species = SPECIES.get(entity.getType());
        if (species != null && species.shape() != null) {
            species.shape().apply(bend, rig, model);
            return;
        }
        long seed = entity.getUUID().getLeastSignificantBits();
        float grown = 1.06F + chance(seed, 1) * 0.2F;
        float tilt = (chance(seed, 2) - 0.5F) * 0.4F;
        for (ModelPart head : rig.head()) {
            bend.scale(head, grown, grown, grown);
            bend.turn(head, 0, 0, tilt);
        }
        if (model instanceof HumanoidModel<?> human) {
            bend.scale(human.hat, grown, grown, grown);
            bend.turn(human.hat, 0, 0, tilt);
        }
        List<ModelPart> limbs = rig.limbs();
        if (!limbs.isEmpty()) {
            ModelPart swollen = limbs.get((int) (chance(seed, 3) * limbs.size()));
            ModelPart withered = limbs.get((int) (chance(seed, 4) * limbs.size()));
            bend.scale(swollen, 1.4F, 1.08F, 1.4F);
            if (withered != swollen) {
                bend.scale(withered, 0.72F, 1F, 0.72F);
            }
        }
        if (!rig.body().isEmpty()) {
            bend.scale(rig.body().get(0), 1.08F + chance(seed, 5) * 0.14F, 1F, 1.05F + chance(seed, 6) * 0.12F);
        }
    }

    public static void restore(EntityModel<?> model) {
        Bend top = BENDS.peek();
        if (top != null && top.model == model) {
            BENDS.pop().undo();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof LivingEntityRenderer living) {
                living.addLayer(new Layer(living));
            }
        }
    }

    private static void growths(ModelPart host, long seed, int salt, PoseStack pose, VertexConsumer out, int light, int overlay, int colour) {
        boolean[] done = {false};
        host.visit(pose, (at, path, index, cube) -> {
            if (done[0] || !path.isEmpty()) {
                return;
            }
            done[0] = true;
            float small = Math.min(cube.maxX - cube.minX, Math.min(cube.maxY - cube.minY, cube.maxZ - cube.minZ));
            for (int i = 0; i < 3; i++) {
                int face = (int) (chance(seed, salt + i * 3) * 5);
                float a = 0.2F + 0.6F * chance(seed, salt + i * 3 + 1);
                float b = 0.2F + 0.6F * chance(seed, salt + i * 3 + 2);
                SCRATCH.pushPose();
                SCRATCH.last().pose().set(at.pose());
                SCRATCH.last().normal().set(at.normal());
                switch (face) {
                    case 1 -> {
                        SCRATCH.translate(Mth.lerp(a, cube.minX, cube.maxX) / 16F, Mth.lerp(b, cube.minY, cube.maxY) / 16F, cube.minZ / 16F);
                        SCRATCH.mulPose(Axis.XP.rotationDegrees(90));
                    }
                    case 2 -> {
                        SCRATCH.translate(Mth.lerp(a, cube.minX, cube.maxX) / 16F, Mth.lerp(b, cube.minY, cube.maxY) / 16F, cube.maxZ / 16F);
                        SCRATCH.mulPose(Axis.XP.rotationDegrees(-90));
                    }
                    case 3 -> {
                        SCRATCH.translate(cube.minX / 16F, Mth.lerp(a, cube.minY, cube.maxY) / 16F, Mth.lerp(b, cube.minZ, cube.maxZ) / 16F);
                        SCRATCH.mulPose(Axis.ZP.rotationDegrees(-90));
                    }
                    case 4 -> {
                        SCRATCH.translate(cube.maxX / 16F, Mth.lerp(a, cube.minY, cube.maxY) / 16F, Mth.lerp(b, cube.minZ, cube.maxZ) / 16F);
                        SCRATCH.mulPose(Axis.ZP.rotationDegrees(90));
                    }
                    default -> SCRATCH.translate(Mth.lerp(a, cube.minX, cube.maxX) / 16F, cube.minY / 16F, Mth.lerp(b, cube.minZ, cube.maxZ) / 16F);
                }
                (i == 2 ? SPUR : i == 0 && small >= 4 ? BIG_BOIL : BOIL).render(SCRATCH, out, light, overlay, colour);
                SCRATCH.popPose();
            }
        });
    }

    static final class Layer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
        Layer(RenderLayerParent<T, M> parent) {
            super(parent);
        }

        private void twin(Twin twin, Rig rig, PoseStack pose, VertexConsumer out, int light, int overlay, int colour) {
            ModelPart part = twin.source().of(rig);
            if (part == null) {
                return;
            }
            PartPose kept = part.storePose();
            float x = part.xScale;
            float y = part.yScale;
            float z = part.zScale;
            part.x += twin.x();
            part.y += twin.y();
            part.z += twin.z();
            part.xRot = part.xRot * twin.swing() + twin.xRot();
            part.yRot += twin.yRot();
            part.zRot += twin.zRot();
            part.xScale *= twin.scale();
            part.yScale *= twin.scale();
            part.zScale *= twin.scale();
            part.render(pose, out, light, overlay, colour);
            part.loadPose(kept);
            part.xScale = x;
            part.yScale = y;
            part.zScale = z;
        }

        private void extras(@Nullable Species species, Rig rig, long seed, PoseStack pose, VertexConsumer out, int light, int overlay, int colour) {
            if (species != null) {
                for (Graft graft : species.grafts()) {
                    ModelPart host = graft.host().of(rig);
                    if (host != null) {
                        pose.pushPose();
                        host.translateAndRotate(pose);
                        graft.part().render(pose, out, light, overlay, colour);
                        pose.popPose();
                    }
                }
            }
            ModelPart torso = rig.torso();
            if (torso != null) {
                growths(torso, seed, 20, pose, out, light, overlay, colour);
            }
            if (!rig.head().isEmpty()) {
                growths(rig.head().get(0), seed, 40, pose, out, light, overlay, colour);
            }
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible() || !shows(entity)) {
                return;
            }
            M model = getParentModel();
            MutantSkins.Skin skin = MutantSkins.of(getTextureLocation(entity));
            int level = (int) (255F * Mth.clamp(0.6F + 0.2F * Mth.sin((age + entity.getId() * 7.3F) * 0.12F), 0F, 1F));
            int lit = FastColor.ARGB32.color(255, level, level, level);
            if (skin.glow() != null) {
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(skin.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lit);
            }
            if (entity.isBaby()) {
                return;
            }
            Rig rig = rig(model);
            Species species = SPECIES.get(entity.getType());
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
            long seed = entity.getUUID().getLeastSignificantBits();
            if (species != null && !species.twins().isEmpty()) {
                VertexConsumer hide = buffers.getBuffer(model.renderType(skin.colour()));
                for (Twin twin : species.twins()) {
                    twin(twin, rig, pose, hide, light, overlay, -1);
                }
                if (skin.glow() != null) {
                    VertexConsumer shine = buffers.getBuffer(RenderType.eyes(skin.glow()));
                    for (Twin twin : species.twins()) {
                        twin(twin, rig, pose, shine, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lit);
                    }
                }
            }
            extras(species, rig, seed, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(GROWTHS)), light, overlay, -1);
            extras(species, rig, seed, pose, buffers.getBuffer(RenderType.eyes(GROWTHS_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lit);
        }
    }
}
