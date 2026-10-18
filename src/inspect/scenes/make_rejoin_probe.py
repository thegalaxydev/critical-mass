from body_common import loop, write

O = (100, -59, 0)
cmds, fix, fill, run = loop(O)
steps = [{"cmds": ["/time set 6000", "/gamemode spectator"], "wait": 10, "gui": False},
         {"cmds": cmds, "wait": 10}, {"cmds": fix, "wait": 10}, {"cmds": fill + run, "wait": 100},
         {"cmds": ["/criticalmass ledger"], "wait": 5},
         {"rejoin": True, "wait": 10},
         {"cmds": ["/criticalmass ledger"], "wait": 5}]
write("rejoin_probe", steps)
