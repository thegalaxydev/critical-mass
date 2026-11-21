package com.criticalmass.gametest;

import com.criticalmass.CriticalMass;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.ServerAdvancementManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CriticalMass.MODID)
@PrefixGameTestTemplate(false)
public final class AdvancementTests {
    private static final List<String> MAIN = List.of("root", "concentrate", "enriched", "cascade", "pellet", "assembly", "vessel", "critical", "full_power", "steam", "on_grid", "order_met",
            "spent", "dry_storage", "reprocessed", "canister", "desk", "screen", "meter", "suit", "dose", "bypass", "core_damage", "explosion", "chartrine", "grown", "monitor", "cleanup");
    private static final List<String> RECOVERY = List.of("root", "corium", "wreckage", "scrub", "debris", "all_clear", "charred");

    private AdvancementTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void everyAdvancementLoadsUnderOneRoot(GameTestHelper h) {
        ServerAdvancementManager all = h.getLevel().getServer().getAdvancements();
        int roots = 0;
        for (String folder : List.of("main", "recovery")) {
            for (String name : folder.equals("main") ? MAIN : RECOVERY) {
                AdvancementHolder one = all.get(CriticalMass.asResource(folder + "/" + name));
                h.assertTrue(one != null, "The advancement " + folder + "/" + name + " should load");
                h.assertTrue(one.value().display().isPresent(), folder + "/" + name + " should have a title and an icon");
                if (one.value().parent().isEmpty()) {
                    roots++;
                } else {
                    h.assertTrue(all.get(one.value().parent().get()) != null, folder + "/" + name + " should hang from an advancement that exists: " + one.value().parent().get());
                }
            }
        }
        h.assertTrue(roots == 1, "The advancements should be one tree under one root: " + roots);
        h.succeed();
    }
}
