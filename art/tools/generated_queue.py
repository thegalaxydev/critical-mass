import datetime
import os
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
LOG = os.path.join(os.path.dirname(HERE), "art", "queue_log.txt")

BLOCKS = ["coolant_channel", "reactor_service_port", "primary_coolant_port", "reinforced_pipe", "heat_exchanger", "condenser",
          "cooling_tower_packing", "radiator_panel", "emergency_coolant_tank", "relief_valve", "boiler_heating_plate",
          "shielding_concrete", "shielded_glass", "shielded_hatch", "fuel_assembly_bench", "spent_fuel_rack", "shielded_cask",
          "reactor_signal_interface", "alarm_siren", "docking_service_connector", "control_rod_drive", "control_rod_drive_pinion"]
ITEMS = ["raw_uranium", "crushed_uranium", "uranium_concentrate", "ceramic_fuel_pellet", "fuel_cladding", "reactor_alloy_ingot",
         "reactor_alloy_sheet", "sealing_gasket", "dosimeter",
         "decontamination_compound", "repair_kit"]
FLUIDS = ["primary_coolant", "steam", "contaminated_coolant"]


def jobs():
    out = []
    for n in BLOCKS:
        if not os.path.exists(os.path.join(os.path.dirname(HERE), "art", "final", n + ".png")):
            out.append(["generated", n])
    for n in ITEMS:
        if not os.path.exists(os.path.join(os.path.dirname(HERE), "art", "final", "item__" + n + ".png")):
            out.append(["generated_item", n])
    for n in FLUIDS:
        if not os.path.exists(os.path.join(os.path.dirname(HERE), "art", "final", "fluid__" + n + ".png")):
            out.append(["generated_fluid", n])
    return out


def log(msg):
    with open(LOG, "a", encoding="utf8") as f:
        f.write(datetime.datetime.now().strftime("%H:%M:%S ") + msg + "\n")


stop = False


def work(job):
    global stop
    if stop:
        log("skip " + " ".join(job))
        return
    r = subprocess.run([sys.executable, os.path.join(HERE, "gen.py")] + job + ["--v", "1", "--force"], capture_output=True, text=True, encoding="utf8", errors="replace", cwd=HERE)
    text = (r.stdout + r.stderr).strip().replace("\n", " | ")
    if "usage limit" in text:
        stop = True
    log(("OK   " if "final" in r.stdout else "FAIL ") + " ".join(job) + " :: " + text[-300:])


if __name__ == "__main__":
    start = sys.argv[1] if len(sys.argv) > 1 else None
    if start:
        h, m = map(int, start.split(":"))
        while datetime.datetime.now().time() < datetime.time(h, m):
            time.sleep(20)
    js = jobs()
    log("start %d jobs" % len(js))
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(work, js))
    log("done stop=%s" % stop)
