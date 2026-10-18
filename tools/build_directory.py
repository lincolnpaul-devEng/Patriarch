#!/usr/bin/env python3
"""Builds the onboarding directory assets (countries, regions, universities).

Source: Hipo/university-domains-list (MIT License) plus curated featured campuses
and the official 47 Kenyan counties. Run from the repo root:

    python3 tools/build_directory.py /path/to/world_universities_and_domains.json
"""
import json
import re
import sys
from pathlib import Path

SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/uni/full.json")
OUT = Path("core/data/src/main/assets/directory")
OUT.mkdir(parents=True, exist_ok=True)

KENYA_COUNTIES = [
    "Mombasa", "Kwale", "Kilifi", "Tana River", "Lamu", "Taita-Taveta", "Garissa", "Wajir",
    "Mandera", "Marsabit", "Isiolo", "Meru", "Tharaka-Nithi", "Embu", "Kitui", "Machakos",
    "Makueni", "Nyandarua", "Nyeri", "Kirinyaga", "Murang'a", "Kiambu", "Turkana", "West Pokot",
    "Samburu", "Trans-Nzoia", "Uasin Gishu", "Elgeyo-Marakwet", "Nandi", "Baringo", "Laikipia",
    "Nakuru", "Narok", "Kajiado", "Kericho", "Bomet", "Kakamega", "Vihiga", "Bungoma", "Busia",
    "Siaya", "Kisumu", "Homa Bay", "Migori", "Kisii", "Nyamira", "Nairobi",
]

US_STATES = {
    "AL": "Alabama", "AK": "Alaska", "AZ": "Arizona", "AR": "Arkansas", "CA": "California",
    "CO": "Colorado", "CT": "Connecticut", "DE": "Delaware", "DC": "District of Columbia",
    "FL": "Florida", "GA": "Georgia", "HI": "Hawaii", "ID": "Idaho", "IL": "Illinois",
    "IN": "Indiana", "IA": "Iowa", "KS": "Kansas", "KY": "Kentucky", "LA": "Louisiana",
    "ME": "Maine", "MD": "Maryland", "MA": "Massachusetts", "MI": "Michigan", "MN": "Minnesota",
    "MS": "Mississippi", "MO": "Missouri", "MT": "Montana", "NE": "Nebraska", "NV": "Nevada",
    "NH": "New Hampshire", "NJ": "New Jersey", "NM": "New Mexico", "NY": "New York",
    "NC": "North Carolina", "ND": "North Dakota", "OH": "Ohio", "OK": "Oklahoma", "OR": "Oregon",
    "PA": "Pennsylvania", "RI": "Rhode Island", "SC": "South Carolina", "SD": "South Dakota",
    "TN": "Tennessee", "TX": "Texas", "UT": "Utah", "VT": "Vermont", "VA": "Virginia",
    "WA": "Washington", "WV": "West Virginia", "WI": "Wisconsin", "WY": "Wyoming",
}
UK_NATIONS = ["England", "Scotland", "Wales", "Northern Ireland"]

KENYA_NAME_TO_COUNTY = {
    "Africa International University": "Nairobi", "Africa Nazarene University": "Kajiado",
    "Adventist University of Africa": "Kajiado", "Chuka University": "Tharaka-Nithi",
    "Catholic University of Eastern Africa": "Nairobi", "Daystar University": "Machakos",
    "Dedan Kimathi University of Technology": "Nyeri", "Egerton University": "Nakuru",
    "Great Lakes University of Kisumu": "Kisumu", "Inoorero University": "Nairobi",
    "Jaramogi Oginga Odinga University of Science and Technology": "Siaya",
    "Kabarak University": "Nakuru", "University of Kabianga": "Kericho",
    "Karatina University": "Nyeri", "Kenya College of Accountancy": "Nairobi",
    "Kenya Methodist University": "Meru", "Kisii University": "Kisii",
    "Kenya Medical Training College": "Nairobi", "Kiriri Womens University of Science and Technology": "Nairobi",
    "Laikipia University": "Laikipia", "Maseno University": "Kisumu",
    "Maasai Mara University": "Narok", "Masinde Muliro University of Science and Technology": "Kakamega",
    "Meru University of Science and Technology": "Meru", "Nairobi Institute of Business Studies": "Nairobi",
    "Nairobi Technical Training Institute": "Nairobi", "Pan Africa Christian University": "Nairobi",
    "Pioneer International University": "Nairobi", "Pwani University": "Kilifi",
    "Presbyterian University of East Africa": "Kiambu", "Riara University School of Business and Law": "Nairobi",
    "Scott Christian University": "Machakos", "South Eastern Kenya University": "Kitui",
    "St. Pauls University": "Kiambu", "University of Eastern Africa, Baraton": "Nandi",
    "University of Eldoret": "Uasin Gishu", "Technical University of Mombasa": "Mombasa",
    "Uzima University College": "Kisumu", "Zetech College": "Kiambu", "Gretsa Universtiy": "Kiambu",
}

# (name, country code, region, domain). Order within a country = display order. Not a ranking.
FEATURED = [
    ("University of Nairobi", "KE", "Nairobi", "uonbi.ac.ke"),
    ("Kenyatta University", "KE", "Nairobi", "ku.ac.ke"),
    ("Strathmore University", "KE", "Nairobi", "strathmore.edu"),
    ("United States International University - Africa", "KE", "Nairobi", "usiu.ac.ke"),
    ("Technical University of Kenya", "KE", "Nairobi", "tukenya.ac.ke"),
    ("Multimedia University of Kenya", "KE", "Nairobi", "mmu.ac.ke"),
    ("Jomo Kenyatta University of Agriculture and Technology", "KE", "Kiambu", "jkuat.ac.ke"),
    ("Mount Kenya University", "KE", "Kiambu", "mku.ac.ke"),
    ("Moi University", "KE", "Uasin Gishu", "mu.ac.ke"),
    ("Egerton University", "KE", "Nakuru", "egerton.ac.ke"),
    ("Maseno University", "KE", "Kisumu", "maseno.ac.ke"),
    ("Dedan Kimathi University of Technology", "KE", "Nyeri", "dkut.ac.ke"),
    ("Massachusetts Institute of Technology", "US", "Massachusetts", "mit.edu"),
    ("Stanford University", "US", "California", "stanford.edu"),
    ("Harvard University", "US", "Massachusetts", "harvard.edu"),
    ("University of California, Berkeley", "US", "California", "berkeley.edu"),
    ("California Institute of Technology", "US", "California", "caltech.edu"),
    ("Princeton University", "US", "New Jersey", "princeton.edu"),
    ("Yale University", "US", "Connecticut", "yale.edu"),
    ("Columbia University", "US", "New York", "columbia.edu"),
    ("University of Chicago", "US", "Illinois", "uchicago.edu"),
    ("University of Pennsylvania", "US", "Pennsylvania", "upenn.edu"),
    ("Cornell University", "US", "New York", "cornell.edu"),
    ("University of Michigan", "US", "Michigan", "umich.edu"),
    ("New York University", "US", "New York", "nyu.edu"),
    ("University of California, Los Angeles", "US", "California", "ucla.edu"),
    ("University of Texas at Austin", "US", "Texas", "utexas.edu"),
    ("Georgia Institute of Technology", "US", "Georgia", "gatech.edu"),
    ("University of Oxford", "GB", "England", "ox.ac.uk"),
    ("University of Cambridge", "GB", "England", "cam.ac.uk"),
    ("Imperial College London", "GB", "England", "imperial.ac.uk"),
    ("University College London", "GB", "England", "ucl.ac.uk"),
    ("University of Edinburgh", "GB", "Scotland", "ed.ac.uk"),
    ("University of Manchester", "GB", "England", "manchester.ac.uk"),
    ("King's College London", "GB", "England", "kcl.ac.uk"),
    ("London School of Economics", "GB", "England", "lse.ac.uk"),
    ("University of Ibadan", "NG", "Oyo", "ui.edu.ng"),
    ("University of Lagos", "NG", "Lagos", "unilag.edu.ng"),
    ("Obafemi Awolowo University", "NG", "Osun", "oauife.edu.ng"),
    ("Covenant University", "NG", "Ogun", "covenantuniversity.edu.ng"),
    ("Ahmadu Bello University", "NG", "Kaduna", "abu.edu.ng"),
    ("University of Cape Town", "ZA", "Western Cape", "uct.ac.za"),
    ("University of the Witwatersrand", "ZA", "Gauteng", "wits.ac.za"),
    ("Stellenbosch University", "ZA", "Western Cape", "sun.ac.za"),
    ("University of Pretoria", "ZA", "Gauteng", "up.ac.za"),
    ("University of KwaZulu-Natal", "ZA", "KwaZulu-Natal", "ukzn.ac.za"),
    ("Makerere University", "UG", "Central", "mak.ac.ug"),
    ("University of Dar es Salaam", "TZ", "Dar es Salaam", "udsm.ac.tz"),
    ("University of Rwanda", "RW", "Kigali", "ur.ac.rw"),
    ("University of Ghana", "GH", "Greater Accra", "ug.edu.gh"),
    ("Kwame Nkrumah University of Science and Technology", "GH", "Ashanti", "knust.edu.gh"),
    ("Addis Ababa University", "ET", "Addis Ababa", "aau.edu.et"),
    ("University of Toronto", "CA", "Ontario", "utoronto.ca"),
    ("University of British Columbia", "CA", "British Columbia", "ubc.ca"),
    ("McGill University", "CA", "Quebec", "mcgill.ca"),
    ("University of Waterloo", "CA", "Ontario", "uwaterloo.ca"),
    ("University of Melbourne", "AU", "Victoria", "unimelb.edu.au"),
    ("University of Sydney", "AU", "New South Wales", "sydney.edu.au"),
    ("Australian National University", "AU", "Australian Capital Territory", "anu.edu.au"),
    ("Indian Institute of Technology Bombay", "IN", "Maharashtra", "iitb.ac.in"),
    ("Indian Institute of Technology Delhi", "IN", "Delhi", "iitd.ac.in"),
    ("Indian Institute of Science", "IN", "Karnataka", "iisc.ac.in"),
    ("National University of Singapore", "SG", "Singapore", "nus.edu.sg"),
    ("Technical University of Munich", "DE", "Bavaria", "tum.de"),
]

# Featured countries first, in this order, then A-Z.
COUNTRY_PRIORITY = ["KE", "US", "GB", "NG", "ZA", "UG", "TZ", "RW", "GH", "ET", "CA", "AU", "IN", "SG", "DE"]
CURRENCY = {
    "KE": "KES", "US": "USD", "GB": "GBP", "NG": "NGN", "ZA": "ZAR", "UG": "UGX", "TZ": "TZS",
    "RW": "RWF", "GH": "GHS", "ET": "ETB", "CA": "CAD", "AU": "AUD", "IN": "INR", "SG": "SGD",
    "DE": "EUR", "FR": "EUR", "IT": "EUR", "ES": "EUR", "NL": "EUR", "IE": "EUR", "JP": "JPY",
    "CN": "CNY", "BR": "BRL", "MX": "MXN", "AE": "AED", "EG": "EGP", "ZM": "ZMW", "ZW": "USD",
}


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())


def clean_region(code, raw):
    if not raw or raw.strip().lower() in ("null", "none", ""):
        return ""
    raw = raw.strip()
    if code == "US":
        up = raw.upper()
        if up in US_STATES:
            return US_STATES[up]
        for full in US_STATES.values():
            if full.lower() == raw.lower():
                return full
        return ""
    if code == "KE":
        for c in KENYA_COUNTIES:
            if c.lower() == raw.lower():
                return c
        return ""
    return raw


data = json.loads(SRC.read_text())
entries = {}  # key -> dict
by_domain = {}
by_name = {}
countries = {}
for x in data:
    code = x.get("alpha_two_code") or ""
    if not code:
        continue
    countries.setdefault(code, x["country"])
    domains = x.get("domains") or []
    region = clean_region(code, x.get("state-province"))
    if code == "KE" and not region:
        region = KENYA_NAME_TO_COUNTY.get(x["name"], "")
    e = {"n": x["name"].strip(), "c": code, "r": region, "d": domains[0] if domains else "", "f": 0}
    key = (code, norm(e["n"]))
    if key in entries:
        continue
    entries[key] = e
    for d in domains:
        by_domain[d.lower()] = e
    by_name[key] = e

featured_rank = {}
for i, (name, code, region, domain) in enumerate(FEATURED):
    e = by_domain.get(domain) or by_name.get((code, norm(name)))
    if e is None:
        e = {"n": name, "c": code, "r": region, "d": domain, "f": 0}
        entries[(code, norm(name))] = e
    e["n"], e["r"], e["f"] = name, region, i + 1
    countries.setdefault(code, code)

# Regions per country
regions = {"KE": KENYA_COUNTIES, "US": list(US_STATES.values()), "GB": UK_NATIONS}
for e in entries.values():
    if e["c"] in ("KE", "US", "GB"):
        continue
    if e["r"]:
        regions.setdefault(e["c"], set()).add(e["r"])
regions = {k: (v if isinstance(v, list) else sorted(v)) for k, v in regions.items()}
regions = {k: v for k, v in regions.items() if len(v) > 1}

order = {c: i for i, c in enumerate(COUNTRY_PRIORITY)}
country_list = sorted(countries.items(), key=lambda kv: (order.get(kv[0], 999), kv[1]))
countries_out = [
    {"code": c, "name": n, "cur": CURRENCY.get(c, "USD"), "feat": 1 if c in order else 0}
    for c, n in country_list
]

unis = sorted(entries.values(), key=lambda e: (e["c"], 0 if e["f"] else 1, e["f"], e["n"]))
(OUT / "countries.json").write_text(json.dumps(countries_out, ensure_ascii=False, separators=(",", ":")))
(OUT / "regions.json").write_text(json.dumps(regions, ensure_ascii=False, separators=(",", ":")))
(OUT / "universities.json").write_text(json.dumps(unis, ensure_ascii=False, separators=(",", ":")))
print(len(countries_out), "countries;", len(regions), "countries with regions;", len(unis), "universities")
print("featured:", sum(1 for e in unis if e["f"]))
