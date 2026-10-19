from body_common import ASSEMBLER, ASSEMBLER_OFFSET, loop, write

O = (108, -60, -3)
cmds, fix, fill, run = loop(O)


def w(x, y, z):
    return "%d %d %d" % (x + O[0], y + O[1], z + O[2])


AREA = "%s %s" % (w(0, 0, 0), w(7, 6, 7))
ASSEMBLER_CMD = "/setblock %s simulated:physics_assembler" % w(*ASSEMBLER)
OFFSET = ASSEMBLER_OFFSET
L = {"cmds": ["/criticalmass ledger", "/sable info @n"], "wait": 5}

steps = [{"cmds": ["/time set 6000", "/gamerule doDaylightCycle false", "/gamemode spectator", "/fill 80 -61 -40 260 -61 40 minecraft:smooth_stone"], "wait": 10, "gui": False},
         {"cmds": cmds + [ASSEMBLER_CMD], "wait": 10}, {"cmds": fix, "wait": 10}, {"cmds": fill + run, "wait": 100},
         {"cmds": ["/tp @p 104 -50 -12 -30 35"], "wait": 20},
         {"note": "SECTION tsk-006 cycles"}, {"cmds": ["/criticalmass ledger"], "wait": 5}, {"shot": "start"}]
for i in range(10):
    steps += [{"note": "cycle %d assemble" % i}, {"cmds": ["/sable assemble area " + AREA], "wait": 40}, L,
              {"note": "cycle %d disassemble" % i}, {"invoke": {"offset": OFFSET, "method": "assembleOrDisassemble", "far": True}, "wait": 120}, L]
steps += [{"shot": "after_cycles"},
          {"note": "SECTION tsk-007 reload disassembled"}, L, {"rejoin": True, "wait": 5}, L, {"wait": 40}, L,
          {"note": "SECTION tsk-005 moving"}, {"cmds": ["/sable assemble area " + AREA], "wait": 40}, L,
          {"cmds": ["/sable paused true", "/sable teleport @n 112 -45 1", "/tp @p 100 -42 -14 -40 15"], "wait": 20},
          {"cmds": ["/sable paused false", "/sable physics impulse @n angular 0 0 60 global", "/sable physics impulse @n angular 40 0 0 global"], "wait": 3},
          {"shot": "moving_a"}, L, {"wait": 6}, {"shot": "moving_b"}, L, {"wait": 120}, L,
          {"cmds": ["/tp @p 104 -54 -10 -30 25"], "wait": 20}, {"shot": "landed"},
          {"note": "SECTION tsk-008 chunks"}]
for x in (127, 143, 159, 175, 191, 207, 223):
    steps += [{"cmds": ["/sable paused true", "/sable teleport @n %d -58 %d" % (x, 15 if (x // 16) % 2 else -1)], "wait": 2},
              {"cmds": ["/sable paused false"], "wait": 30}, L]
steps += [{"cmds": ["/tp @p 215 -52 -12 -30 30"], "wait": 30}, {"shot": "far_chunks"},
          {"note": "SECTION tsk-007 reload assembled"}, {"cmds": ["/criticalmass ledger"], "wait": 1}, {"rejoin": True, "wait": 5}, L, {"wait": 40}, L,
          {"wait": 5}]
write("body_tests", steps)
