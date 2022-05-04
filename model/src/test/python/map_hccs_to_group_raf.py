# CR (CRV2 aka CRV05) RAF Update, EN-3509
# spreadsheet with raf & group info:
# https://docs.google.com/spreadsheets/d/1hBQf5_vxUabZNiiRf2d2irTiOBOTQz11zFJE-pqidyU/edit#gid=1946083949
import json
from collections import defaultdict

model_path = '../main/resources/hcc-model.json'

tmp = open(model_path, 'r')
model_json = json.loads(tmp.read())
tmp.close()

version_counts = defaultdict(int)
zero_raf_counts = defaultdict(int)
for hcc in model_json:
    version_counts[hcc['v']] += 1
    if hcc.get('r', 0.0) == 0.0:
        zero_raf_counts[hcc['v']] += 1

# version_counts
# defaultdict(<class 'int'>,
#             {   'CRV1': 127,
#                 'CRV2': 128,
#                 'RX': 76,
#                 'V12': 70,
#                 'V22': 79,
#                 'V23': 83})
# zero_raf_counts
# defaultdict(<class 'int'>, {'RX': 4, 'CRV1': 53, 'CRV2': 53})

update_raf = [hcc['c'] for hcc in model_json if hcc.get('r', 0.0) == 0.0 and hcc['v'] == 'CRV2']

# from definition column from table 6
group_definitions = """if HHS_HCC019   = 1 then do; HHS_HCC019   = 0; G01     = 1; end;
if HHS_HCC020   = 1 then do; HHS_HCC020   = 0; G01     = 1; end;
if HHS_HCC021   = 1 then do; HHS_HCC021   = 0; G01     = 1; end;

if HHS_HCC026   = 1 then do; HHS_HCC026   = 0; G02A    = 1; end;
if HHS_HCC027   = 1 then do; HHS_HCC027   = 0; G02A    = 1; end;
if HHS_HCC029   = 1 then do; HHS_HCC029   = 0; G02A    = 1; end;
if HHS_HCC030   = 1 then do; HHS_HCC030   = 0; G02A    = 1; end;

if HHS_HCC054   = 1 then do; HHS_HCC054   = 0; G03     = 1; end;
if HHS_HCC055   = 1 then do; HHS_HCC055   = 0; G03     = 1; end;

if HHS_HCC061   = 1 then do; HHS_HCC061   = 0; G04     = 1; end;
if HHS_HCC062   = 1 then do; HHS_HCC062   = 0; G04     = 1; end;

if HHS_HCC067   = 1 then do; HHS_HCC067   = 0; G06     = 1; end;
if HHS_HCC068   = 1 then do; HHS_HCC068   = 0; G06     = 1; end;

if HHS_HCC069   = 1 then do; HHS_HCC069   = 0; G07     = 1; end;
if HHS_HCC070   = 1 then do; HHS_HCC070   = 0; G07     = 1; end;
if HHS_HCC071   = 1 then do; HHS_HCC071   = 0; G07     = 1; end;

if HHS_HCC073   = 1 then do; HHS_HCC073   = 0; G08     = 1; end;
if HHS_HCC074   = 1 then do; HHS_HCC074   = 0; G08     = 1; end;

if HHS_HCC081   = 1 then do; HHS_HCC081   = 0; G09     = 1; end;
if HHS_HCC082   = 1 then do; HHS_HCC082   = 0; G09     = 1; end;

if HHS_HCC106   = 1 then do; HHS_HCC106   = 0; G10     = 1; end;
if HHS_HCC107   = 1 then do; HHS_HCC107   = 0; G10     = 1; end;

if HHS_HCC108   = 1 then do; HHS_HCC108   = 0; G11     = 1; end;
if HHS_HCC109   = 1 then do; HHS_HCC109   = 0; G11     = 1; end;

if HHS_HCC117   = 1 then do; HHS_HCC117   = 0; G12     = 1; end;
if HHS_HCC119   = 1 then do; HHS_HCC119   = 0; G12     = 1; end;

if HHS_HCC126   = 1 then do; HHS_HCC126   = 0; G13     = 1; end;
if HHS_HCC127   = 1 then do; HHS_HCC127   = 0; G13     = 1; end;

if HHS_HCC128   = 1 then do; HHS_HCC128   = 0; G14     = 1; end;
if HHS_HCC129   = 1 then do; HHS_HCC129   = 0; G14     = 1; end;

if HHS_HCC160   = 1 then do; HHS_HCC160   = 0; G15     = 1; end;
if HHS_HCC161   = 1 then do; HHS_HCC161   = 0; G15     = 1; end;

if HHS_HCC187   = 1 then do; HHS_HCC187   = 0; G16     = 1; end;
if HHS_HCC188   = 1 then do; HHS_HCC188   = 0; G16     = 1; end;

if HHS_HCC203   = 1 then do; HHS_HCC203   = 0; G17     = 1; end;
if HHS_HCC204   = 1 then do; HHS_HCC204   = 0; G17     = 1; end;
if HHS_HCC205   = 1 then do; HHS_HCC205   = 0; G17     = 1; end;

if HHS_HCC207   = 1 then do; HHS_HCC207   = 0; G18     = 1; end;
if HHS_HCC208   = 1 then do; HHS_HCC208   = 0; G18     = 1; end;
if HHS_HCC209   = 1 then do; HHS_HCC209   = 0; G18     = 1; end;""".replace('\n\n', '\n').split('\n')

# from table 9, platinum level
group_raf = """G01	Yes	0.659
G02A	Yes	2.119
G03	Yes	5.834
G04	Yes	2.844
G06	Yes	11.445
G07	Yes	7.920
G08	Yes	4.883
G09	Yes	3.860
G10	Yes	10.376
G11	Yes	8.240
G12	Yes	2.091
G13	Yes	9.110
G14	Yes	29.057
G15	Yes	0.896
G16	Yes	1.512
G17	Yes	1.259
G18	Yes	3.421""".split('\n')

group_raf_mapping = defaultdict(float)
for g in group_raf:
    group, raf = g.split('Yes')[0], g.split('Yes')[1]
    group_raf_mapping[group] = float(raf)

hcc_raf_mapping = defaultdict(float)
for d in group_definitions:
    code = str(int(d.split()[1].replace('HHS_HCC', '')))
    group = d.split()[9]
    hcc_raf_mapping[code] = group_raf_mapping[group]

print("||hcc||group raf||")
for hcc in sorted(update_raf, key=lambda x: int(x)):
    print("|{}|{}|".format(hcc, hcc_raf_mapping[hcc]))
# ||hcc||group raf||
# |19|0.659|
# |20|0.659|
# |21|0.659|
# |26|2.119|
# |27|2.119|
# |28|0.0|
# |29|2.119|
# |30|2.119|
# |54|5.834|
# |55|5.834|
# |61|2.844|
# |62|2.844|
# |64|0.0|
# |67|11.445|
# |68|11.445|
# |69|7.92|
# |70|7.92|
# |71|7.92|
# |73|4.883|
# |74|4.883|
# |81|3.86|
# |82|3.86|
# |106|10.376|
# |107|10.376|
# |108|8.24|
# |109|8.24|
# |117|2.091|
# |119|2.091|
# |126|9.11|
# |127|0.0|
# |128|0.0|
# |129|29.057|
# |137|0.0|
# |138|0.0|
# |139|0.0|
# |160|0.896|
# |161|0.896|
# |187|1.512|
# |188|1.512|
# |203|1.259|
# |204|1.259|
# |205|0.0|
# |207|0.0|
# |208|0.0|
# |209|0.0|
# |242|0.0|
# |243|0.0|
# |244|0.0|
# |245|0.0|
# |246|0.0|
# |247|0.0|
# |248|0.0|
# |249|0.0|