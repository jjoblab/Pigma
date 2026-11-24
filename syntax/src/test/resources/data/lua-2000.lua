-- Corpus Lua de 2000 lignes : constructions variées,
-- longues chaînes et commentaires qui traversent les fenêtres.

local corpus = {}

--[==[ Bloc 0 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc0 = {
  cle0 = 1, -- hexa 0x1, flottant 0.143, exposant 1e-2
  cle1 = 4, -- hexa 0x4, flottant 0.571, exposant 4e-2
  cle2 = 7, -- hexa 0x7, flottant 1.000, exposant 7e-2
  cle3 = 10, -- hexa 0xa, flottant 1.429, exposant 10e-2
  cle4 = 13, -- hexa 0xd, flottant 1.857, exposant 13e-2
  cle5 = 16, -- hexa 0x10, flottant 2.286, exposant 16e-2
  cle6 = 19, -- hexa 0x13, flottant 2.714, exposant 19e-2
  cle7 = 22, -- hexa 0x16, flottant 3.143, exposant 22e-2
}
function corpus.somme0(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte0 = [[
Longue chaîne du bloc 0, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue0 = [==[
Niveau 2 du bloc 0 : ]] et ]=] restent dans la chaîne.
]==]
  if bloc0.cle1 ~= nil then
    print('bloc 0', "total", bloc0.cle1)
  end

--[==[ Bloc 1 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc1 = {
  cle0 = 25, -- hexa 0x19, flottant 3.571, exposant 25e-2
  cle1 = 28, -- hexa 0x1c, flottant 4.000, exposant 28e-2
  cle2 = 31, -- hexa 0x1f, flottant 4.429, exposant 31e-2
  cle3 = 34, -- hexa 0x22, flottant 4.857, exposant 34e-2
  cle4 = 37, -- hexa 0x25, flottant 5.286, exposant 37e-2
  cle5 = 40, -- hexa 0x28, flottant 5.714, exposant 40e-2
  cle6 = 43, -- hexa 0x2b, flottant 6.143, exposant 43e-2
  cle7 = 46, -- hexa 0x2e, flottant 6.571, exposant 46e-2
}
function corpus.somme1(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte1 = [[
Longue chaîne du bloc 1, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 2 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc2 = {
  cle0 = 49, -- hexa 0x31, flottant 7.000, exposant 49e-2
  cle1 = 52, -- hexa 0x34, flottant 7.429, exposant 52e-2
  cle2 = 55, -- hexa 0x37, flottant 7.857, exposant 55e-2
  cle3 = 58, -- hexa 0x3a, flottant 8.286, exposant 58e-2
  cle4 = 61, -- hexa 0x3d, flottant 8.714, exposant 61e-2
  cle5 = 64, -- hexa 0x40, flottant 9.143, exposant 64e-2
  cle6 = 67, -- hexa 0x43, flottant 9.571, exposant 67e-2
  cle7 = 70, -- hexa 0x46, flottant 10.000, exposant 70e-2
}
function corpus.somme2(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte2 = [[
Longue chaîne du bloc 2, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 3 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc3 = {
  cle0 = 73, -- hexa 0x49, flottant 10.429, exposant 73e-2
  cle1 = 76, -- hexa 0x4c, flottant 10.857, exposant 76e-2
  cle2 = 79, -- hexa 0x4f, flottant 11.286, exposant 79e-2
  cle3 = 82, -- hexa 0x52, flottant 11.714, exposant 82e-2
  cle4 = 85, -- hexa 0x55, flottant 12.143, exposant 85e-2
  cle5 = 88, -- hexa 0x58, flottant 12.571, exposant 88e-2
  cle6 = 91, -- hexa 0x5b, flottant 13.000, exposant 91e-2
  cle7 = 94, -- hexa 0x5e, flottant 13.429, exposant 94e-2
}
function corpus.somme3(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte3 = [[
Longue chaîne du bloc 3, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 4 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc4 = {
  cle0 = 97, -- hexa 0x61, flottant 13.857, exposant 97e-2
  cle1 = 100, -- hexa 0x64, flottant 14.286, exposant 100e-2
  cle2 = 103, -- hexa 0x67, flottant 14.714, exposant 103e-2
  cle3 = 106, -- hexa 0x6a, flottant 15.143, exposant 106e-2
  cle4 = 109, -- hexa 0x6d, flottant 15.571, exposant 109e-2
  cle5 = 112, -- hexa 0x70, flottant 16.000, exposant 112e-2
  cle6 = 115, -- hexa 0x73, flottant 16.429, exposant 115e-2
  cle7 = 118, -- hexa 0x76, flottant 16.857, exposant 118e-2
}
function corpus.somme4(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte4 = [[
Longue chaîne du bloc 4, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 5 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc5 = {
  cle0 = 121, -- hexa 0x79, flottant 17.286, exposant 121e-2
  cle1 = 124, -- hexa 0x7c, flottant 17.714, exposant 124e-2
  cle2 = 127, -- hexa 0x7f, flottant 18.143, exposant 127e-2
  cle3 = 130, -- hexa 0x82, flottant 18.571, exposant 130e-2
  cle4 = 133, -- hexa 0x85, flottant 19.000, exposant 133e-2
  cle5 = 136, -- hexa 0x88, flottant 19.429, exposant 136e-2
  cle6 = 139, -- hexa 0x8b, flottant 19.857, exposant 139e-2
  cle7 = 142, -- hexa 0x8e, flottant 20.286, exposant 142e-2
}
function corpus.somme5(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte5 = [[
Longue chaîne du bloc 5, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue5 = [==[
Niveau 2 du bloc 5 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 6 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc6 = {
  cle0 = 145, -- hexa 0x91, flottant 20.714, exposant 145e-2
  cle1 = 148, -- hexa 0x94, flottant 21.143, exposant 148e-2
  cle2 = 151, -- hexa 0x97, flottant 21.571, exposant 151e-2
  cle3 = 154, -- hexa 0x9a, flottant 22.000, exposant 154e-2
  cle4 = 157, -- hexa 0x9d, flottant 22.429, exposant 157e-2
  cle5 = 160, -- hexa 0xa0, flottant 22.857, exposant 160e-2
  cle6 = 163, -- hexa 0xa3, flottant 23.286, exposant 163e-2
  cle7 = 166, -- hexa 0xa6, flottant 23.714, exposant 166e-2
}
function corpus.somme6(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte6 = [[
Longue chaîne du bloc 6, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 7 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc7 = {
  cle0 = 169, -- hexa 0xa9, flottant 24.143, exposant 169e-2
  cle1 = 172, -- hexa 0xac, flottant 24.571, exposant 172e-2
  cle2 = 175, -- hexa 0xaf, flottant 25.000, exposant 175e-2
  cle3 = 178, -- hexa 0xb2, flottant 25.429, exposant 178e-2
  cle4 = 181, -- hexa 0xb5, flottant 25.857, exposant 181e-2
  cle5 = 184, -- hexa 0xb8, flottant 26.286, exposant 184e-2
  cle6 = 187, -- hexa 0xbb, flottant 26.714, exposant 187e-2
  cle7 = 190, -- hexa 0xbe, flottant 27.143, exposant 190e-2
}
function corpus.somme7(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte7 = [[
Longue chaîne du bloc 7, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc7.cle1 ~= nil then
    print('bloc 7', "total", bloc7.cle1)
  end

--[==[ Bloc 8 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc8 = {
  cle0 = 193, -- hexa 0xc1, flottant 27.571, exposant 193e-2
  cle1 = 196, -- hexa 0xc4, flottant 28.000, exposant 196e-2
  cle2 = 199, -- hexa 0xc7, flottant 28.429, exposant 199e-2
  cle3 = 202, -- hexa 0xca, flottant 28.857, exposant 202e-2
  cle4 = 205, -- hexa 0xcd, flottant 29.286, exposant 205e-2
  cle5 = 208, -- hexa 0xd0, flottant 29.714, exposant 208e-2
  cle6 = 211, -- hexa 0xd3, flottant 30.143, exposant 211e-2
  cle7 = 214, -- hexa 0xd6, flottant 30.571, exposant 214e-2
}
function corpus.somme8(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte8 = [[
Longue chaîne du bloc 8, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 9 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc9 = {
  cle0 = 217, -- hexa 0xd9, flottant 31.000, exposant 217e-2
  cle1 = 220, -- hexa 0xdc, flottant 31.429, exposant 220e-2
  cle2 = 223, -- hexa 0xdf, flottant 31.857, exposant 223e-2
  cle3 = 226, -- hexa 0xe2, flottant 32.286, exposant 226e-2
  cle4 = 229, -- hexa 0xe5, flottant 32.714, exposant 229e-2
  cle5 = 232, -- hexa 0xe8, flottant 33.143, exposant 232e-2
  cle6 = 235, -- hexa 0xeb, flottant 33.571, exposant 235e-2
  cle7 = 238, -- hexa 0xee, flottant 34.000, exposant 238e-2
}
function corpus.somme9(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte9 = [[
Longue chaîne du bloc 9, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 10 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc10 = {
  cle0 = 241, -- hexa 0xf1, flottant 34.429, exposant 241e-2
  cle1 = 244, -- hexa 0xf4, flottant 34.857, exposant 244e-2
  cle2 = 247, -- hexa 0xf7, flottant 35.286, exposant 247e-2
  cle3 = 250, -- hexa 0xfa, flottant 35.714, exposant 250e-2
  cle4 = 253, -- hexa 0xfd, flottant 36.143, exposant 253e-2
  cle5 = 256, -- hexa 0x100, flottant 36.571, exposant 256e-2
  cle6 = 259, -- hexa 0x103, flottant 37.000, exposant 259e-2
  cle7 = 262, -- hexa 0x106, flottant 37.429, exposant 262e-2
}
function corpus.somme10(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte10 = [[
Longue chaîne du bloc 10, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue10 = [==[
Niveau 2 du bloc 10 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 11 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc11 = {
  cle0 = 265, -- hexa 0x109, flottant 37.857, exposant 265e-2
  cle1 = 268, -- hexa 0x10c, flottant 38.286, exposant 268e-2
  cle2 = 271, -- hexa 0x10f, flottant 38.714, exposant 271e-2
  cle3 = 274, -- hexa 0x112, flottant 39.143, exposant 274e-2
  cle4 = 277, -- hexa 0x115, flottant 39.571, exposant 277e-2
  cle5 = 280, -- hexa 0x118, flottant 40.000, exposant 280e-2
  cle6 = 283, -- hexa 0x11b, flottant 40.429, exposant 283e-2
  cle7 = 286, -- hexa 0x11e, flottant 40.857, exposant 286e-2
}
function corpus.somme11(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte11 = [[
Longue chaîne du bloc 11, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 12 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc12 = {
  cle0 = 289, -- hexa 0x121, flottant 41.286, exposant 289e-2
  cle1 = 292, -- hexa 0x124, flottant 41.714, exposant 292e-2
  cle2 = 295, -- hexa 0x127, flottant 42.143, exposant 295e-2
  cle3 = 298, -- hexa 0x12a, flottant 42.571, exposant 298e-2
  cle4 = 301, -- hexa 0x12d, flottant 43.000, exposant 301e-2
  cle5 = 304, -- hexa 0x130, flottant 43.429, exposant 304e-2
  cle6 = 307, -- hexa 0x133, flottant 43.857, exposant 307e-2
  cle7 = 310, -- hexa 0x136, flottant 44.286, exposant 310e-2
}
function corpus.somme12(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte12 = [[
Longue chaîne du bloc 12, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 13 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc13 = {
  cle0 = 313, -- hexa 0x139, flottant 44.714, exposant 313e-2
  cle1 = 316, -- hexa 0x13c, flottant 45.143, exposant 316e-2
  cle2 = 319, -- hexa 0x13f, flottant 45.571, exposant 319e-2
  cle3 = 322, -- hexa 0x142, flottant 46.000, exposant 322e-2
  cle4 = 325, -- hexa 0x145, flottant 46.429, exposant 325e-2
  cle5 = 328, -- hexa 0x148, flottant 46.857, exposant 328e-2
  cle6 = 331, -- hexa 0x14b, flottant 47.286, exposant 331e-2
  cle7 = 334, -- hexa 0x14e, flottant 47.714, exposant 334e-2
}
function corpus.somme13(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte13 = [[
Longue chaîne du bloc 13, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 14 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc14 = {
  cle0 = 337, -- hexa 0x151, flottant 48.143, exposant 337e-2
  cle1 = 340, -- hexa 0x154, flottant 48.571, exposant 340e-2
  cle2 = 343, -- hexa 0x157, flottant 49.000, exposant 343e-2
  cle3 = 346, -- hexa 0x15a, flottant 49.429, exposant 346e-2
  cle4 = 349, -- hexa 0x15d, flottant 49.857, exposant 349e-2
  cle5 = 352, -- hexa 0x160, flottant 50.286, exposant 352e-2
  cle6 = 355, -- hexa 0x163, flottant 50.714, exposant 355e-2
  cle7 = 358, -- hexa 0x166, flottant 51.143, exposant 358e-2
}
function corpus.somme14(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte14 = [[
Longue chaîne du bloc 14, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc14.cle1 ~= nil then
    print('bloc 14', "total", bloc14.cle1)
  end

--[==[ Bloc 15 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc15 = {
  cle0 = 361, -- hexa 0x169, flottant 51.571, exposant 361e-2
  cle1 = 364, -- hexa 0x16c, flottant 52.000, exposant 364e-2
  cle2 = 367, -- hexa 0x16f, flottant 52.429, exposant 367e-2
  cle3 = 370, -- hexa 0x172, flottant 52.857, exposant 370e-2
  cle4 = 373, -- hexa 0x175, flottant 53.286, exposant 373e-2
  cle5 = 376, -- hexa 0x178, flottant 53.714, exposant 376e-2
  cle6 = 379, -- hexa 0x17b, flottant 54.143, exposant 379e-2
  cle7 = 382, -- hexa 0x17e, flottant 54.571, exposant 382e-2
}
function corpus.somme15(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte15 = [[
Longue chaîne du bloc 15, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue15 = [==[
Niveau 2 du bloc 15 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 16 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc16 = {
  cle0 = 385, -- hexa 0x181, flottant 55.000, exposant 385e-2
  cle1 = 388, -- hexa 0x184, flottant 55.429, exposant 388e-2
  cle2 = 391, -- hexa 0x187, flottant 55.857, exposant 391e-2
  cle3 = 394, -- hexa 0x18a, flottant 56.286, exposant 394e-2
  cle4 = 397, -- hexa 0x18d, flottant 56.714, exposant 397e-2
  cle5 = 400, -- hexa 0x190, flottant 57.143, exposant 400e-2
  cle6 = 403, -- hexa 0x193, flottant 57.571, exposant 403e-2
  cle7 = 406, -- hexa 0x196, flottant 58.000, exposant 406e-2
}
function corpus.somme16(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte16 = [[
Longue chaîne du bloc 16, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 17 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc17 = {
  cle0 = 409, -- hexa 0x199, flottant 58.429, exposant 409e-2
  cle1 = 412, -- hexa 0x19c, flottant 58.857, exposant 412e-2
  cle2 = 415, -- hexa 0x19f, flottant 59.286, exposant 415e-2
  cle3 = 418, -- hexa 0x1a2, flottant 59.714, exposant 418e-2
  cle4 = 421, -- hexa 0x1a5, flottant 60.143, exposant 421e-2
  cle5 = 424, -- hexa 0x1a8, flottant 60.571, exposant 424e-2
  cle6 = 427, -- hexa 0x1ab, flottant 61.000, exposant 427e-2
  cle7 = 430, -- hexa 0x1ae, flottant 61.429, exposant 430e-2
}
function corpus.somme17(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte17 = [[
Longue chaîne du bloc 17, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 18 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc18 = {
  cle0 = 433, -- hexa 0x1b1, flottant 61.857, exposant 433e-2
  cle1 = 436, -- hexa 0x1b4, flottant 62.286, exposant 436e-2
  cle2 = 439, -- hexa 0x1b7, flottant 62.714, exposant 439e-2
  cle3 = 442, -- hexa 0x1ba, flottant 63.143, exposant 442e-2
  cle4 = 445, -- hexa 0x1bd, flottant 63.571, exposant 445e-2
  cle5 = 448, -- hexa 0x1c0, flottant 64.000, exposant 448e-2
  cle6 = 451, -- hexa 0x1c3, flottant 64.429, exposant 451e-2
  cle7 = 454, -- hexa 0x1c6, flottant 64.857, exposant 454e-2
}
function corpus.somme18(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte18 = [[
Longue chaîne du bloc 18, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 19 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc19 = {
  cle0 = 457, -- hexa 0x1c9, flottant 65.286, exposant 457e-2
  cle1 = 460, -- hexa 0x1cc, flottant 65.714, exposant 460e-2
  cle2 = 463, -- hexa 0x1cf, flottant 66.143, exposant 463e-2
  cle3 = 466, -- hexa 0x1d2, flottant 66.571, exposant 466e-2
  cle4 = 469, -- hexa 0x1d5, flottant 67.000, exposant 469e-2
  cle5 = 472, -- hexa 0x1d8, flottant 67.429, exposant 472e-2
  cle6 = 475, -- hexa 0x1db, flottant 67.857, exposant 475e-2
  cle7 = 478, -- hexa 0x1de, flottant 68.286, exposant 478e-2
}
function corpus.somme19(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte19 = [[
Longue chaîne du bloc 19, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 20 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc20 = {
  cle0 = 481, -- hexa 0x1e1, flottant 68.714, exposant 481e-2
  cle1 = 484, -- hexa 0x1e4, flottant 69.143, exposant 484e-2
  cle2 = 487, -- hexa 0x1e7, flottant 69.571, exposant 487e-2
  cle3 = 490, -- hexa 0x1ea, flottant 70.000, exposant 490e-2
  cle4 = 493, -- hexa 0x1ed, flottant 70.429, exposant 493e-2
  cle5 = 496, -- hexa 0x1f0, flottant 70.857, exposant 496e-2
  cle6 = 499, -- hexa 0x1f3, flottant 71.286, exposant 499e-2
  cle7 = 502, -- hexa 0x1f6, flottant 71.714, exposant 502e-2
}
function corpus.somme20(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte20 = [[
Longue chaîne du bloc 20, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue20 = [==[
Niveau 2 du bloc 20 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 21 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc21 = {
  cle0 = 505, -- hexa 0x1f9, flottant 72.143, exposant 505e-2
  cle1 = 508, -- hexa 0x1fc, flottant 72.571, exposant 508e-2
  cle2 = 511, -- hexa 0x1ff, flottant 73.000, exposant 511e-2
  cle3 = 514, -- hexa 0x202, flottant 73.429, exposant 514e-2
  cle4 = 517, -- hexa 0x205, flottant 73.857, exposant 517e-2
  cle5 = 520, -- hexa 0x208, flottant 74.286, exposant 520e-2
  cle6 = 523, -- hexa 0x20b, flottant 74.714, exposant 523e-2
  cle7 = 526, -- hexa 0x20e, flottant 75.143, exposant 526e-2
}
function corpus.somme21(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte21 = [[
Longue chaîne du bloc 21, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc21.cle1 ~= nil then
    print('bloc 21', "total", bloc21.cle1)
  end

--[==[ Bloc 22 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc22 = {
  cle0 = 529, -- hexa 0x211, flottant 75.571, exposant 529e-2
  cle1 = 532, -- hexa 0x214, flottant 76.000, exposant 532e-2
  cle2 = 535, -- hexa 0x217, flottant 76.429, exposant 535e-2
  cle3 = 538, -- hexa 0x21a, flottant 76.857, exposant 538e-2
  cle4 = 541, -- hexa 0x21d, flottant 77.286, exposant 541e-2
  cle5 = 544, -- hexa 0x220, flottant 77.714, exposant 544e-2
  cle6 = 547, -- hexa 0x223, flottant 78.143, exposant 547e-2
  cle7 = 550, -- hexa 0x226, flottant 78.571, exposant 550e-2
}
function corpus.somme22(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte22 = [[
Longue chaîne du bloc 22, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 23 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc23 = {
  cle0 = 553, -- hexa 0x229, flottant 79.000, exposant 553e-2
  cle1 = 556, -- hexa 0x22c, flottant 79.429, exposant 556e-2
  cle2 = 559, -- hexa 0x22f, flottant 79.857, exposant 559e-2
  cle3 = 562, -- hexa 0x232, flottant 80.286, exposant 562e-2
  cle4 = 565, -- hexa 0x235, flottant 80.714, exposant 565e-2
  cle5 = 568, -- hexa 0x238, flottant 81.143, exposant 568e-2
  cle6 = 571, -- hexa 0x23b, flottant 81.571, exposant 571e-2
  cle7 = 574, -- hexa 0x23e, flottant 82.000, exposant 574e-2
}
function corpus.somme23(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte23 = [[
Longue chaîne du bloc 23, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 24 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc24 = {
  cle0 = 577, -- hexa 0x241, flottant 82.429, exposant 577e-2
  cle1 = 580, -- hexa 0x244, flottant 82.857, exposant 580e-2
  cle2 = 583, -- hexa 0x247, flottant 83.286, exposant 583e-2
  cle3 = 586, -- hexa 0x24a, flottant 83.714, exposant 586e-2
  cle4 = 589, -- hexa 0x24d, flottant 84.143, exposant 589e-2
  cle5 = 592, -- hexa 0x250, flottant 84.571, exposant 592e-2
  cle6 = 595, -- hexa 0x253, flottant 85.000, exposant 595e-2
  cle7 = 598, -- hexa 0x256, flottant 85.429, exposant 598e-2
}
function corpus.somme24(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte24 = [[
Longue chaîne du bloc 24, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 25 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc25 = {
  cle0 = 601, -- hexa 0x259, flottant 85.857, exposant 601e-2
  cle1 = 604, -- hexa 0x25c, flottant 86.286, exposant 604e-2
  cle2 = 607, -- hexa 0x25f, flottant 86.714, exposant 607e-2
  cle3 = 610, -- hexa 0x262, flottant 87.143, exposant 610e-2
  cle4 = 613, -- hexa 0x265, flottant 87.571, exposant 613e-2
  cle5 = 616, -- hexa 0x268, flottant 88.000, exposant 616e-2
  cle6 = 619, -- hexa 0x26b, flottant 88.429, exposant 619e-2
  cle7 = 622, -- hexa 0x26e, flottant 88.857, exposant 622e-2
}
function corpus.somme25(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte25 = [[
Longue chaîne du bloc 25, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue25 = [==[
Niveau 2 du bloc 25 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 26 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc26 = {
  cle0 = 625, -- hexa 0x271, flottant 89.286, exposant 625e-2
  cle1 = 628, -- hexa 0x274, flottant 89.714, exposant 628e-2
  cle2 = 631, -- hexa 0x277, flottant 90.143, exposant 631e-2
  cle3 = 634, -- hexa 0x27a, flottant 90.571, exposant 634e-2
  cle4 = 637, -- hexa 0x27d, flottant 91.000, exposant 637e-2
  cle5 = 640, -- hexa 0x280, flottant 91.429, exposant 640e-2
  cle6 = 643, -- hexa 0x283, flottant 91.857, exposant 643e-2
  cle7 = 646, -- hexa 0x286, flottant 92.286, exposant 646e-2
}
function corpus.somme26(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte26 = [[
Longue chaîne du bloc 26, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 27 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc27 = {
  cle0 = 649, -- hexa 0x289, flottant 92.714, exposant 649e-2
  cle1 = 652, -- hexa 0x28c, flottant 93.143, exposant 652e-2
  cle2 = 655, -- hexa 0x28f, flottant 93.571, exposant 655e-2
  cle3 = 658, -- hexa 0x292, flottant 94.000, exposant 658e-2
  cle4 = 661, -- hexa 0x295, flottant 94.429, exposant 661e-2
  cle5 = 664, -- hexa 0x298, flottant 94.857, exposant 664e-2
  cle6 = 667, -- hexa 0x29b, flottant 95.286, exposant 667e-2
  cle7 = 670, -- hexa 0x29e, flottant 95.714, exposant 670e-2
}
function corpus.somme27(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte27 = [[
Longue chaîne du bloc 27, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 28 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc28 = {
  cle0 = 673, -- hexa 0x2a1, flottant 96.143, exposant 673e-2
  cle1 = 676, -- hexa 0x2a4, flottant 96.571, exposant 676e-2
  cle2 = 679, -- hexa 0x2a7, flottant 97.000, exposant 679e-2
  cle3 = 682, -- hexa 0x2aa, flottant 97.429, exposant 682e-2
  cle4 = 685, -- hexa 0x2ad, flottant 97.857, exposant 685e-2
  cle5 = 688, -- hexa 0x2b0, flottant 98.286, exposant 688e-2
  cle6 = 691, -- hexa 0x2b3, flottant 98.714, exposant 691e-2
  cle7 = 694, -- hexa 0x2b6, flottant 99.143, exposant 694e-2
}
function corpus.somme28(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte28 = [[
Longue chaîne du bloc 28, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc28.cle1 ~= nil then
    print('bloc 28', "total", bloc28.cle1)
  end

--[==[ Bloc 29 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc29 = {
  cle0 = 697, -- hexa 0x2b9, flottant 99.571, exposant 697e-2
  cle1 = 700, -- hexa 0x2bc, flottant 100.000, exposant 700e-2
  cle2 = 703, -- hexa 0x2bf, flottant 100.429, exposant 703e-2
  cle3 = 706, -- hexa 0x2c2, flottant 100.857, exposant 706e-2
  cle4 = 709, -- hexa 0x2c5, flottant 101.286, exposant 709e-2
  cle5 = 712, -- hexa 0x2c8, flottant 101.714, exposant 712e-2
  cle6 = 715, -- hexa 0x2cb, flottant 102.143, exposant 715e-2
  cle7 = 718, -- hexa 0x2ce, flottant 102.571, exposant 718e-2
}
function corpus.somme29(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte29 = [[
Longue chaîne du bloc 29, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 30 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc30 = {
  cle0 = 721, -- hexa 0x2d1, flottant 103.000, exposant 721e-2
  cle1 = 724, -- hexa 0x2d4, flottant 103.429, exposant 724e-2
  cle2 = 727, -- hexa 0x2d7, flottant 103.857, exposant 727e-2
  cle3 = 730, -- hexa 0x2da, flottant 104.286, exposant 730e-2
  cle4 = 733, -- hexa 0x2dd, flottant 104.714, exposant 733e-2
  cle5 = 736, -- hexa 0x2e0, flottant 105.143, exposant 736e-2
  cle6 = 739, -- hexa 0x2e3, flottant 105.571, exposant 739e-2
  cle7 = 742, -- hexa 0x2e6, flottant 106.000, exposant 742e-2
}
function corpus.somme30(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte30 = [[
Longue chaîne du bloc 30, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue30 = [==[
Niveau 2 du bloc 30 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 31 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc31 = {
  cle0 = 745, -- hexa 0x2e9, flottant 106.429, exposant 745e-2
  cle1 = 748, -- hexa 0x2ec, flottant 106.857, exposant 748e-2
  cle2 = 751, -- hexa 0x2ef, flottant 107.286, exposant 751e-2
  cle3 = 754, -- hexa 0x2f2, flottant 107.714, exposant 754e-2
  cle4 = 757, -- hexa 0x2f5, flottant 108.143, exposant 757e-2
  cle5 = 760, -- hexa 0x2f8, flottant 108.571, exposant 760e-2
  cle6 = 763, -- hexa 0x2fb, flottant 109.000, exposant 763e-2
  cle7 = 766, -- hexa 0x2fe, flottant 109.429, exposant 766e-2
}
function corpus.somme31(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte31 = [[
Longue chaîne du bloc 31, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 32 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc32 = {
  cle0 = 769, -- hexa 0x301, flottant 109.857, exposant 769e-2
  cle1 = 772, -- hexa 0x304, flottant 110.286, exposant 772e-2
  cle2 = 775, -- hexa 0x307, flottant 110.714, exposant 775e-2
  cle3 = 778, -- hexa 0x30a, flottant 111.143, exposant 778e-2
  cle4 = 781, -- hexa 0x30d, flottant 111.571, exposant 781e-2
  cle5 = 784, -- hexa 0x310, flottant 112.000, exposant 784e-2
  cle6 = 787, -- hexa 0x313, flottant 112.429, exposant 787e-2
  cle7 = 790, -- hexa 0x316, flottant 112.857, exposant 790e-2
}
function corpus.somme32(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte32 = [[
Longue chaîne du bloc 32, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 33 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc33 = {
  cle0 = 793, -- hexa 0x319, flottant 113.286, exposant 793e-2
  cle1 = 796, -- hexa 0x31c, flottant 113.714, exposant 796e-2
  cle2 = 799, -- hexa 0x31f, flottant 114.143, exposant 799e-2
  cle3 = 802, -- hexa 0x322, flottant 114.571, exposant 802e-2
  cle4 = 805, -- hexa 0x325, flottant 115.000, exposant 805e-2
  cle5 = 808, -- hexa 0x328, flottant 115.429, exposant 808e-2
  cle6 = 811, -- hexa 0x32b, flottant 115.857, exposant 811e-2
  cle7 = 814, -- hexa 0x32e, flottant 116.286, exposant 814e-2
}
function corpus.somme33(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte33 = [[
Longue chaîne du bloc 33, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 34 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc34 = {
  cle0 = 817, -- hexa 0x331, flottant 116.714, exposant 817e-2
  cle1 = 820, -- hexa 0x334, flottant 117.143, exposant 820e-2
  cle2 = 823, -- hexa 0x337, flottant 117.571, exposant 823e-2
  cle3 = 826, -- hexa 0x33a, flottant 118.000, exposant 826e-2
  cle4 = 829, -- hexa 0x33d, flottant 118.429, exposant 829e-2
  cle5 = 832, -- hexa 0x340, flottant 118.857, exposant 832e-2
  cle6 = 835, -- hexa 0x343, flottant 119.286, exposant 835e-2
  cle7 = 838, -- hexa 0x346, flottant 119.714, exposant 838e-2
}
function corpus.somme34(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte34 = [[
Longue chaîne du bloc 34, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 35 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc35 = {
  cle0 = 841, -- hexa 0x349, flottant 120.143, exposant 841e-2
  cle1 = 844, -- hexa 0x34c, flottant 120.571, exposant 844e-2
  cle2 = 847, -- hexa 0x34f, flottant 121.000, exposant 847e-2
  cle3 = 850, -- hexa 0x352, flottant 121.429, exposant 850e-2
  cle4 = 853, -- hexa 0x355, flottant 121.857, exposant 853e-2
  cle5 = 856, -- hexa 0x358, flottant 122.286, exposant 856e-2
  cle6 = 859, -- hexa 0x35b, flottant 122.714, exposant 859e-2
  cle7 = 862, -- hexa 0x35e, flottant 123.143, exposant 862e-2
}
function corpus.somme35(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte35 = [[
Longue chaîne du bloc 35, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue35 = [==[
Niveau 2 du bloc 35 : ]] et ]=] restent dans la chaîne.
]==]
  if bloc35.cle1 ~= nil then
    print('bloc 35', "total", bloc35.cle1)
  end

--[==[ Bloc 36 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc36 = {
  cle0 = 865, -- hexa 0x361, flottant 123.571, exposant 865e-2
  cle1 = 868, -- hexa 0x364, flottant 124.000, exposant 868e-2
  cle2 = 871, -- hexa 0x367, flottant 124.429, exposant 871e-2
  cle3 = 874, -- hexa 0x36a, flottant 124.857, exposant 874e-2
  cle4 = 877, -- hexa 0x36d, flottant 125.286, exposant 877e-2
  cle5 = 880, -- hexa 0x370, flottant 125.714, exposant 880e-2
  cle6 = 883, -- hexa 0x373, flottant 126.143, exposant 883e-2
  cle7 = 886, -- hexa 0x376, flottant 126.571, exposant 886e-2
}
function corpus.somme36(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte36 = [[
Longue chaîne du bloc 36, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 37 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc37 = {
  cle0 = 889, -- hexa 0x379, flottant 127.000, exposant 889e-2
  cle1 = 892, -- hexa 0x37c, flottant 127.429, exposant 892e-2
  cle2 = 895, -- hexa 0x37f, flottant 127.857, exposant 895e-2
  cle3 = 898, -- hexa 0x382, flottant 128.286, exposant 898e-2
  cle4 = 901, -- hexa 0x385, flottant 128.714, exposant 901e-2
  cle5 = 904, -- hexa 0x388, flottant 129.143, exposant 904e-2
  cle6 = 907, -- hexa 0x38b, flottant 129.571, exposant 907e-2
  cle7 = 910, -- hexa 0x38e, flottant 130.000, exposant 910e-2
}
function corpus.somme37(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte37 = [[
Longue chaîne du bloc 37, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 38 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc38 = {
  cle0 = 913, -- hexa 0x391, flottant 130.429, exposant 913e-2
  cle1 = 916, -- hexa 0x394, flottant 130.857, exposant 916e-2
  cle2 = 919, -- hexa 0x397, flottant 131.286, exposant 919e-2
  cle3 = 922, -- hexa 0x39a, flottant 131.714, exposant 922e-2
  cle4 = 925, -- hexa 0x39d, flottant 132.143, exposant 925e-2
  cle5 = 928, -- hexa 0x3a0, flottant 132.571, exposant 928e-2
  cle6 = 931, -- hexa 0x3a3, flottant 133.000, exposant 931e-2
  cle7 = 934, -- hexa 0x3a6, flottant 133.429, exposant 934e-2
}
function corpus.somme38(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte38 = [[
Longue chaîne du bloc 38, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 39 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc39 = {
  cle0 = 937, -- hexa 0x3a9, flottant 133.857, exposant 937e-2
  cle1 = 940, -- hexa 0x3ac, flottant 134.286, exposant 940e-2
  cle2 = 943, -- hexa 0x3af, flottant 134.714, exposant 943e-2
  cle3 = 946, -- hexa 0x3b2, flottant 135.143, exposant 946e-2
  cle4 = 949, -- hexa 0x3b5, flottant 135.571, exposant 949e-2
  cle5 = 952, -- hexa 0x3b8, flottant 136.000, exposant 952e-2
  cle6 = 955, -- hexa 0x3bb, flottant 136.429, exposant 955e-2
  cle7 = 958, -- hexa 0x3be, flottant 136.857, exposant 958e-2
}
function corpus.somme39(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte39 = [[
Longue chaîne du bloc 39, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 40 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc40 = {
  cle0 = 961, -- hexa 0x3c1, flottant 137.286, exposant 961e-2
  cle1 = 964, -- hexa 0x3c4, flottant 137.714, exposant 964e-2
  cle2 = 967, -- hexa 0x3c7, flottant 138.143, exposant 967e-2
  cle3 = 970, -- hexa 0x3ca, flottant 138.571, exposant 970e-2
  cle4 = 973, -- hexa 0x3cd, flottant 139.000, exposant 973e-2
  cle5 = 976, -- hexa 0x3d0, flottant 139.429, exposant 976e-2
  cle6 = 979, -- hexa 0x3d3, flottant 139.857, exposant 979e-2
  cle7 = 982, -- hexa 0x3d6, flottant 140.286, exposant 982e-2
}
function corpus.somme40(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte40 = [[
Longue chaîne du bloc 40, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue40 = [==[
Niveau 2 du bloc 40 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 41 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc41 = {
  cle0 = 985, -- hexa 0x3d9, flottant 140.714, exposant 985e-2
  cle1 = 988, -- hexa 0x3dc, flottant 141.143, exposant 988e-2
  cle2 = 991, -- hexa 0x3df, flottant 141.571, exposant 991e-2
  cle3 = 994, -- hexa 0x3e2, flottant 142.000, exposant 994e-2
  cle4 = 997, -- hexa 0x3e5, flottant 142.429, exposant 997e-2
  cle5 = 1000, -- hexa 0x3e8, flottant 142.857, exposant 1000e-2
  cle6 = 1003, -- hexa 0x3eb, flottant 143.286, exposant 1003e-2
  cle7 = 1006, -- hexa 0x3ee, flottant 143.714, exposant 1006e-2
}
function corpus.somme41(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte41 = [[
Longue chaîne du bloc 41, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 42 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc42 = {
  cle0 = 1009, -- hexa 0x3f1, flottant 144.143, exposant 1009e-2
  cle1 = 1012, -- hexa 0x3f4, flottant 144.571, exposant 1012e-2
  cle2 = 1015, -- hexa 0x3f7, flottant 145.000, exposant 1015e-2
  cle3 = 1018, -- hexa 0x3fa, flottant 145.429, exposant 1018e-2
  cle4 = 1021, -- hexa 0x3fd, flottant 145.857, exposant 1021e-2
  cle5 = 1024, -- hexa 0x400, flottant 146.286, exposant 1024e-2
  cle6 = 1027, -- hexa 0x403, flottant 146.714, exposant 1027e-2
  cle7 = 1030, -- hexa 0x406, flottant 147.143, exposant 1030e-2
}
function corpus.somme42(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte42 = [[
Longue chaîne du bloc 42, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc42.cle1 ~= nil then
    print('bloc 42', "total", bloc42.cle1)
  end

--[==[ Bloc 43 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc43 = {
  cle0 = 1033, -- hexa 0x409, flottant 147.571, exposant 1033e-2
  cle1 = 1036, -- hexa 0x40c, flottant 148.000, exposant 1036e-2
  cle2 = 1039, -- hexa 0x40f, flottant 148.429, exposant 1039e-2
  cle3 = 1042, -- hexa 0x412, flottant 148.857, exposant 1042e-2
  cle4 = 1045, -- hexa 0x415, flottant 149.286, exposant 1045e-2
  cle5 = 1048, -- hexa 0x418, flottant 149.714, exposant 1048e-2
  cle6 = 1051, -- hexa 0x41b, flottant 150.143, exposant 1051e-2
  cle7 = 1054, -- hexa 0x41e, flottant 150.571, exposant 1054e-2
}
function corpus.somme43(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte43 = [[
Longue chaîne du bloc 43, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 44 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc44 = {
  cle0 = 1057, -- hexa 0x421, flottant 151.000, exposant 1057e-2
  cle1 = 1060, -- hexa 0x424, flottant 151.429, exposant 1060e-2
  cle2 = 1063, -- hexa 0x427, flottant 151.857, exposant 1063e-2
  cle3 = 1066, -- hexa 0x42a, flottant 152.286, exposant 1066e-2
  cle4 = 1069, -- hexa 0x42d, flottant 152.714, exposant 1069e-2
  cle5 = 1072, -- hexa 0x430, flottant 153.143, exposant 1072e-2
  cle6 = 1075, -- hexa 0x433, flottant 153.571, exposant 1075e-2
  cle7 = 1078, -- hexa 0x436, flottant 154.000, exposant 1078e-2
}
function corpus.somme44(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte44 = [[
Longue chaîne du bloc 44, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 45 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc45 = {
  cle0 = 1081, -- hexa 0x439, flottant 154.429, exposant 1081e-2
  cle1 = 1084, -- hexa 0x43c, flottant 154.857, exposant 1084e-2
  cle2 = 1087, -- hexa 0x43f, flottant 155.286, exposant 1087e-2
  cle3 = 1090, -- hexa 0x442, flottant 155.714, exposant 1090e-2
  cle4 = 1093, -- hexa 0x445, flottant 156.143, exposant 1093e-2
  cle5 = 1096, -- hexa 0x448, flottant 156.571, exposant 1096e-2
  cle6 = 1099, -- hexa 0x44b, flottant 157.000, exposant 1099e-2
  cle7 = 1102, -- hexa 0x44e, flottant 157.429, exposant 1102e-2
}
function corpus.somme45(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte45 = [[
Longue chaîne du bloc 45, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue45 = [==[
Niveau 2 du bloc 45 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 46 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc46 = {
  cle0 = 1105, -- hexa 0x451, flottant 157.857, exposant 1105e-2
  cle1 = 1108, -- hexa 0x454, flottant 158.286, exposant 1108e-2
  cle2 = 1111, -- hexa 0x457, flottant 158.714, exposant 1111e-2
  cle3 = 1114, -- hexa 0x45a, flottant 159.143, exposant 1114e-2
  cle4 = 1117, -- hexa 0x45d, flottant 159.571, exposant 1117e-2
  cle5 = 1120, -- hexa 0x460, flottant 160.000, exposant 1120e-2
  cle6 = 1123, -- hexa 0x463, flottant 160.429, exposant 1123e-2
  cle7 = 1126, -- hexa 0x466, flottant 160.857, exposant 1126e-2
}
function corpus.somme46(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte46 = [[
Longue chaîne du bloc 46, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 47 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc47 = {
  cle0 = 1129, -- hexa 0x469, flottant 161.286, exposant 1129e-2
  cle1 = 1132, -- hexa 0x46c, flottant 161.714, exposant 1132e-2
  cle2 = 1135, -- hexa 0x46f, flottant 162.143, exposant 1135e-2
  cle3 = 1138, -- hexa 0x472, flottant 162.571, exposant 1138e-2
  cle4 = 1141, -- hexa 0x475, flottant 163.000, exposant 1141e-2
  cle5 = 1144, -- hexa 0x478, flottant 163.429, exposant 1144e-2
  cle6 = 1147, -- hexa 0x47b, flottant 163.857, exposant 1147e-2
  cle7 = 1150, -- hexa 0x47e, flottant 164.286, exposant 1150e-2
}
function corpus.somme47(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte47 = [[
Longue chaîne du bloc 47, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 48 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc48 = {
  cle0 = 1153, -- hexa 0x481, flottant 164.714, exposant 1153e-2
  cle1 = 1156, -- hexa 0x484, flottant 165.143, exposant 1156e-2
  cle2 = 1159, -- hexa 0x487, flottant 165.571, exposant 1159e-2
  cle3 = 1162, -- hexa 0x48a, flottant 166.000, exposant 1162e-2
  cle4 = 1165, -- hexa 0x48d, flottant 166.429, exposant 1165e-2
  cle5 = 1168, -- hexa 0x490, flottant 166.857, exposant 1168e-2
  cle6 = 1171, -- hexa 0x493, flottant 167.286, exposant 1171e-2
  cle7 = 1174, -- hexa 0x496, flottant 167.714, exposant 1174e-2
}
function corpus.somme48(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte48 = [[
Longue chaîne du bloc 48, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 49 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc49 = {
  cle0 = 1177, -- hexa 0x499, flottant 168.143, exposant 1177e-2
  cle1 = 1180, -- hexa 0x49c, flottant 168.571, exposant 1180e-2
  cle2 = 1183, -- hexa 0x49f, flottant 169.000, exposant 1183e-2
  cle3 = 1186, -- hexa 0x4a2, flottant 169.429, exposant 1186e-2
  cle4 = 1189, -- hexa 0x4a5, flottant 169.857, exposant 1189e-2
  cle5 = 1192, -- hexa 0x4a8, flottant 170.286, exposant 1192e-2
  cle6 = 1195, -- hexa 0x4ab, flottant 170.714, exposant 1195e-2
  cle7 = 1198, -- hexa 0x4ae, flottant 171.143, exposant 1198e-2
}
function corpus.somme49(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte49 = [[
Longue chaîne du bloc 49, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc49.cle1 ~= nil then
    print('bloc 49', "total", bloc49.cle1)
  end

--[==[ Bloc 50 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc50 = {
  cle0 = 1201, -- hexa 0x4b1, flottant 171.571, exposant 1201e-2
  cle1 = 1204, -- hexa 0x4b4, flottant 172.000, exposant 1204e-2
  cle2 = 1207, -- hexa 0x4b7, flottant 172.429, exposant 1207e-2
  cle3 = 1210, -- hexa 0x4ba, flottant 172.857, exposant 1210e-2
  cle4 = 1213, -- hexa 0x4bd, flottant 173.286, exposant 1213e-2
  cle5 = 1216, -- hexa 0x4c0, flottant 173.714, exposant 1216e-2
  cle6 = 1219, -- hexa 0x4c3, flottant 174.143, exposant 1219e-2
  cle7 = 1222, -- hexa 0x4c6, flottant 174.571, exposant 1222e-2
}
function corpus.somme50(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte50 = [[
Longue chaîne du bloc 50, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue50 = [==[
Niveau 2 du bloc 50 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 51 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc51 = {
  cle0 = 1225, -- hexa 0x4c9, flottant 175.000, exposant 1225e-2
  cle1 = 1228, -- hexa 0x4cc, flottant 175.429, exposant 1228e-2
  cle2 = 1231, -- hexa 0x4cf, flottant 175.857, exposant 1231e-2
  cle3 = 1234, -- hexa 0x4d2, flottant 176.286, exposant 1234e-2
  cle4 = 1237, -- hexa 0x4d5, flottant 176.714, exposant 1237e-2
  cle5 = 1240, -- hexa 0x4d8, flottant 177.143, exposant 1240e-2
  cle6 = 1243, -- hexa 0x4db, flottant 177.571, exposant 1243e-2
  cle7 = 1246, -- hexa 0x4de, flottant 178.000, exposant 1246e-2
}
function corpus.somme51(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte51 = [[
Longue chaîne du bloc 51, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 52 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc52 = {
  cle0 = 1249, -- hexa 0x4e1, flottant 178.429, exposant 1249e-2
  cle1 = 1252, -- hexa 0x4e4, flottant 178.857, exposant 1252e-2
  cle2 = 1255, -- hexa 0x4e7, flottant 179.286, exposant 1255e-2
  cle3 = 1258, -- hexa 0x4ea, flottant 179.714, exposant 1258e-2
  cle4 = 1261, -- hexa 0x4ed, flottant 180.143, exposant 1261e-2
  cle5 = 1264, -- hexa 0x4f0, flottant 180.571, exposant 1264e-2
  cle6 = 1267, -- hexa 0x4f3, flottant 181.000, exposant 1267e-2
  cle7 = 1270, -- hexa 0x4f6, flottant 181.429, exposant 1270e-2
}
function corpus.somme52(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte52 = [[
Longue chaîne du bloc 52, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 53 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc53 = {
  cle0 = 1273, -- hexa 0x4f9, flottant 181.857, exposant 1273e-2
  cle1 = 1276, -- hexa 0x4fc, flottant 182.286, exposant 1276e-2
  cle2 = 1279, -- hexa 0x4ff, flottant 182.714, exposant 1279e-2
  cle3 = 1282, -- hexa 0x502, flottant 183.143, exposant 1282e-2
  cle4 = 1285, -- hexa 0x505, flottant 183.571, exposant 1285e-2
  cle5 = 1288, -- hexa 0x508, flottant 184.000, exposant 1288e-2
  cle6 = 1291, -- hexa 0x50b, flottant 184.429, exposant 1291e-2
  cle7 = 1294, -- hexa 0x50e, flottant 184.857, exposant 1294e-2
}
function corpus.somme53(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte53 = [[
Longue chaîne du bloc 53, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 54 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc54 = {
  cle0 = 1297, -- hexa 0x511, flottant 185.286, exposant 1297e-2
  cle1 = 1300, -- hexa 0x514, flottant 185.714, exposant 1300e-2
  cle2 = 1303, -- hexa 0x517, flottant 186.143, exposant 1303e-2
  cle3 = 1306, -- hexa 0x51a, flottant 186.571, exposant 1306e-2
  cle4 = 1309, -- hexa 0x51d, flottant 187.000, exposant 1309e-2
  cle5 = 1312, -- hexa 0x520, flottant 187.429, exposant 1312e-2
  cle6 = 1315, -- hexa 0x523, flottant 187.857, exposant 1315e-2
  cle7 = 1318, -- hexa 0x526, flottant 188.286, exposant 1318e-2
}
function corpus.somme54(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte54 = [[
Longue chaîne du bloc 54, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 55 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc55 = {
  cle0 = 1321, -- hexa 0x529, flottant 188.714, exposant 1321e-2
  cle1 = 1324, -- hexa 0x52c, flottant 189.143, exposant 1324e-2
  cle2 = 1327, -- hexa 0x52f, flottant 189.571, exposant 1327e-2
  cle3 = 1330, -- hexa 0x532, flottant 190.000, exposant 1330e-2
  cle4 = 1333, -- hexa 0x535, flottant 190.429, exposant 1333e-2
  cle5 = 1336, -- hexa 0x538, flottant 190.857, exposant 1336e-2
  cle6 = 1339, -- hexa 0x53b, flottant 191.286, exposant 1339e-2
  cle7 = 1342, -- hexa 0x53e, flottant 191.714, exposant 1342e-2
}
function corpus.somme55(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte55 = [[
Longue chaîne du bloc 55, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue55 = [==[
Niveau 2 du bloc 55 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 56 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc56 = {
  cle0 = 1345, -- hexa 0x541, flottant 192.143, exposant 1345e-2
  cle1 = 1348, -- hexa 0x544, flottant 192.571, exposant 1348e-2
  cle2 = 1351, -- hexa 0x547, flottant 193.000, exposant 1351e-2
  cle3 = 1354, -- hexa 0x54a, flottant 193.429, exposant 1354e-2
  cle4 = 1357, -- hexa 0x54d, flottant 193.857, exposant 1357e-2
  cle5 = 1360, -- hexa 0x550, flottant 194.286, exposant 1360e-2
  cle6 = 1363, -- hexa 0x553, flottant 194.714, exposant 1363e-2
  cle7 = 1366, -- hexa 0x556, flottant 195.143, exposant 1366e-2
}
function corpus.somme56(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte56 = [[
Longue chaîne du bloc 56, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc56.cle1 ~= nil then
    print('bloc 56', "total", bloc56.cle1)
  end

--[==[ Bloc 57 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc57 = {
  cle0 = 1369, -- hexa 0x559, flottant 195.571, exposant 1369e-2
  cle1 = 1372, -- hexa 0x55c, flottant 196.000, exposant 1372e-2
  cle2 = 1375, -- hexa 0x55f, flottant 196.429, exposant 1375e-2
  cle3 = 1378, -- hexa 0x562, flottant 196.857, exposant 1378e-2
  cle4 = 1381, -- hexa 0x565, flottant 197.286, exposant 1381e-2
  cle5 = 1384, -- hexa 0x568, flottant 197.714, exposant 1384e-2
  cle6 = 1387, -- hexa 0x56b, flottant 198.143, exposant 1387e-2
  cle7 = 1390, -- hexa 0x56e, flottant 198.571, exposant 1390e-2
}
function corpus.somme57(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte57 = [[
Longue chaîne du bloc 57, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 58 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc58 = {
  cle0 = 1393, -- hexa 0x571, flottant 199.000, exposant 1393e-2
  cle1 = 1396, -- hexa 0x574, flottant 199.429, exposant 1396e-2
  cle2 = 1399, -- hexa 0x577, flottant 199.857, exposant 1399e-2
  cle3 = 1402, -- hexa 0x57a, flottant 200.286, exposant 1402e-2
  cle4 = 1405, -- hexa 0x57d, flottant 200.714, exposant 1405e-2
  cle5 = 1408, -- hexa 0x580, flottant 201.143, exposant 1408e-2
  cle6 = 1411, -- hexa 0x583, flottant 201.571, exposant 1411e-2
  cle7 = 1414, -- hexa 0x586, flottant 202.000, exposant 1414e-2
}
function corpus.somme58(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte58 = [[
Longue chaîne du bloc 58, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 59 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc59 = {
  cle0 = 1417, -- hexa 0x589, flottant 202.429, exposant 1417e-2
  cle1 = 1420, -- hexa 0x58c, flottant 202.857, exposant 1420e-2
  cle2 = 1423, -- hexa 0x58f, flottant 203.286, exposant 1423e-2
  cle3 = 1426, -- hexa 0x592, flottant 203.714, exposant 1426e-2
  cle4 = 1429, -- hexa 0x595, flottant 204.143, exposant 1429e-2
  cle5 = 1432, -- hexa 0x598, flottant 204.571, exposant 1432e-2
  cle6 = 1435, -- hexa 0x59b, flottant 205.000, exposant 1435e-2
  cle7 = 1438, -- hexa 0x59e, flottant 205.429, exposant 1438e-2
}
function corpus.somme59(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte59 = [[
Longue chaîne du bloc 59, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 60 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc60 = {
  cle0 = 1441, -- hexa 0x5a1, flottant 205.857, exposant 1441e-2
  cle1 = 1444, -- hexa 0x5a4, flottant 206.286, exposant 1444e-2
  cle2 = 1447, -- hexa 0x5a7, flottant 206.714, exposant 1447e-2
  cle3 = 1450, -- hexa 0x5aa, flottant 207.143, exposant 1450e-2
  cle4 = 1453, -- hexa 0x5ad, flottant 207.571, exposant 1453e-2
  cle5 = 1456, -- hexa 0x5b0, flottant 208.000, exposant 1456e-2
  cle6 = 1459, -- hexa 0x5b3, flottant 208.429, exposant 1459e-2
  cle7 = 1462, -- hexa 0x5b6, flottant 208.857, exposant 1462e-2
}
function corpus.somme60(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte60 = [[
Longue chaîne du bloc 60, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue60 = [==[
Niveau 2 du bloc 60 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 61 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc61 = {
  cle0 = 1465, -- hexa 0x5b9, flottant 209.286, exposant 1465e-2
  cle1 = 1468, -- hexa 0x5bc, flottant 209.714, exposant 1468e-2
  cle2 = 1471, -- hexa 0x5bf, flottant 210.143, exposant 1471e-2
  cle3 = 1474, -- hexa 0x5c2, flottant 210.571, exposant 1474e-2
  cle4 = 1477, -- hexa 0x5c5, flottant 211.000, exposant 1477e-2
  cle5 = 1480, -- hexa 0x5c8, flottant 211.429, exposant 1480e-2
  cle6 = 1483, -- hexa 0x5cb, flottant 211.857, exposant 1483e-2
  cle7 = 1486, -- hexa 0x5ce, flottant 212.286, exposant 1486e-2
}
function corpus.somme61(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte61 = [[
Longue chaîne du bloc 61, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 62 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc62 = {
  cle0 = 1489, -- hexa 0x5d1, flottant 212.714, exposant 1489e-2
  cle1 = 1492, -- hexa 0x5d4, flottant 213.143, exposant 1492e-2
  cle2 = 1495, -- hexa 0x5d7, flottant 213.571, exposant 1495e-2
  cle3 = 1498, -- hexa 0x5da, flottant 214.000, exposant 1498e-2
  cle4 = 1501, -- hexa 0x5dd, flottant 214.429, exposant 1501e-2
  cle5 = 1504, -- hexa 0x5e0, flottant 214.857, exposant 1504e-2
  cle6 = 1507, -- hexa 0x5e3, flottant 215.286, exposant 1507e-2
  cle7 = 1510, -- hexa 0x5e6, flottant 215.714, exposant 1510e-2
}
function corpus.somme62(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte62 = [[
Longue chaîne du bloc 62, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 63 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc63 = {
  cle0 = 1513, -- hexa 0x5e9, flottant 216.143, exposant 1513e-2
  cle1 = 1516, -- hexa 0x5ec, flottant 216.571, exposant 1516e-2
  cle2 = 1519, -- hexa 0x5ef, flottant 217.000, exposant 1519e-2
  cle3 = 1522, -- hexa 0x5f2, flottant 217.429, exposant 1522e-2
  cle4 = 1525, -- hexa 0x5f5, flottant 217.857, exposant 1525e-2
  cle5 = 1528, -- hexa 0x5f8, flottant 218.286, exposant 1528e-2
  cle6 = 1531, -- hexa 0x5fb, flottant 218.714, exposant 1531e-2
  cle7 = 1534, -- hexa 0x5fe, flottant 219.143, exposant 1534e-2
}
function corpus.somme63(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte63 = [[
Longue chaîne du bloc 63, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
  if bloc63.cle1 ~= nil then
    print('bloc 63', "total", bloc63.cle1)
  end

--[==[ Bloc 64 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc64 = {
  cle0 = 1537, -- hexa 0x601, flottant 219.571, exposant 1537e-2
  cle1 = 1540, -- hexa 0x604, flottant 220.000, exposant 1540e-2
  cle2 = 1543, -- hexa 0x607, flottant 220.429, exposant 1543e-2
  cle3 = 1546, -- hexa 0x60a, flottant 220.857, exposant 1546e-2
  cle4 = 1549, -- hexa 0x60d, flottant 221.286, exposant 1549e-2
  cle5 = 1552, -- hexa 0x610, flottant 221.714, exposant 1552e-2
  cle6 = 1555, -- hexa 0x613, flottant 222.143, exposant 1555e-2
  cle7 = 1558, -- hexa 0x616, flottant 222.571, exposant 1558e-2
}
function corpus.somme64(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte64 = [[
Longue chaîne du bloc 64, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 65 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc65 = {
  cle0 = 1561, -- hexa 0x619, flottant 223.000, exposant 1561e-2
  cle1 = 1564, -- hexa 0x61c, flottant 223.429, exposant 1564e-2
  cle2 = 1567, -- hexa 0x61f, flottant 223.857, exposant 1567e-2
  cle3 = 1570, -- hexa 0x622, flottant 224.286, exposant 1570e-2
  cle4 = 1573, -- hexa 0x625, flottant 224.714, exposant 1573e-2
  cle5 = 1576, -- hexa 0x628, flottant 225.143, exposant 1576e-2
  cle6 = 1579, -- hexa 0x62b, flottant 225.571, exposant 1579e-2
  cle7 = 1582, -- hexa 0x62e, flottant 226.000, exposant 1582e-2
}
function corpus.somme65(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte65 = [[
Longue chaîne du bloc 65, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue65 = [==[
Niveau 2 du bloc 65 : ]] et ]=] restent dans la chaîne.
]==]

--[==[ Bloc 66 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc66 = {
  cle0 = 1585, -- hexa 0x631, flottant 226.429, exposant 1585e-2
  cle1 = 1588, -- hexa 0x634, flottant 226.857, exposant 1588e-2
  cle2 = 1591, -- hexa 0x637, flottant 227.286, exposant 1591e-2
  cle3 = 1594, -- hexa 0x63a, flottant 227.714, exposant 1594e-2
  cle4 = 1597, -- hexa 0x63d, flottant 228.143, exposant 1597e-2
  cle5 = 1600, -- hexa 0x640, flottant 228.571, exposant 1600e-2
  cle6 = 1603, -- hexa 0x643, flottant 229.000, exposant 1603e-2
  cle7 = 1606, -- hexa 0x646, flottant 229.429, exposant 1606e-2
}
function corpus.somme66(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte66 = [[
Longue chaîne du bloc 66, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 67 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc67 = {
  cle0 = 1609, -- hexa 0x649, flottant 229.857, exposant 1609e-2
  cle1 = 1612, -- hexa 0x64c, flottant 230.286, exposant 1612e-2
  cle2 = 1615, -- hexa 0x64f, flottant 230.714, exposant 1615e-2
  cle3 = 1618, -- hexa 0x652, flottant 231.143, exposant 1618e-2
  cle4 = 1621, -- hexa 0x655, flottant 231.571, exposant 1621e-2
  cle5 = 1624, -- hexa 0x658, flottant 232.000, exposant 1624e-2
  cle6 = 1627, -- hexa 0x65b, flottant 232.429, exposant 1627e-2
  cle7 = 1630, -- hexa 0x65e, flottant 232.857, exposant 1630e-2
}
function corpus.somme67(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte67 = [[
Longue chaîne du bloc 67, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 68 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc68 = {
  cle0 = 1633, -- hexa 0x661, flottant 233.286, exposant 1633e-2
  cle1 = 1636, -- hexa 0x664, flottant 233.714, exposant 1636e-2
  cle2 = 1639, -- hexa 0x667, flottant 234.143, exposant 1639e-2
  cle3 = 1642, -- hexa 0x66a, flottant 234.571, exposant 1642e-2
  cle4 = 1645, -- hexa 0x66d, flottant 235.000, exposant 1645e-2
  cle5 = 1648, -- hexa 0x670, flottant 235.429, exposant 1648e-2
  cle6 = 1651, -- hexa 0x673, flottant 235.857, exposant 1651e-2
  cle7 = 1654, -- hexa 0x676, flottant 236.286, exposant 1654e-2
}
function corpus.somme68(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte68 = [[
Longue chaîne du bloc 68, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 69 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc69 = {
  cle0 = 1657, -- hexa 0x679, flottant 236.714, exposant 1657e-2
  cle1 = 1660, -- hexa 0x67c, flottant 237.143, exposant 1660e-2
  cle2 = 1663, -- hexa 0x67f, flottant 237.571, exposant 1663e-2
  cle3 = 1666, -- hexa 0x682, flottant 238.000, exposant 1666e-2
  cle4 = 1669, -- hexa 0x685, flottant 238.429, exposant 1669e-2
  cle5 = 1672, -- hexa 0x688, flottant 238.857, exposant 1672e-2
  cle6 = 1675, -- hexa 0x68b, flottant 239.286, exposant 1675e-2
  cle7 = 1678, -- hexa 0x68e, flottant 239.714, exposant 1678e-2
}
function corpus.somme69(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte69 = [[
Longue chaîne du bloc 69, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 70 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc70 = {
  cle0 = 1681, -- hexa 0x691, flottant 240.143, exposant 1681e-2
  cle1 = 1684, -- hexa 0x694, flottant 240.571, exposant 1684e-2
  cle2 = 1687, -- hexa 0x697, flottant 241.000, exposant 1687e-2
  cle3 = 1690, -- hexa 0x69a, flottant 241.429, exposant 1690e-2
  cle4 = 1693, -- hexa 0x69d, flottant 241.857, exposant 1693e-2
  cle5 = 1696, -- hexa 0x6a0, flottant 242.286, exposant 1696e-2
  cle6 = 1699, -- hexa 0x6a3, flottant 242.714, exposant 1699e-2
  cle7 = 1702, -- hexa 0x6a6, flottant 243.143, exposant 1702e-2
}
function corpus.somme70(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte70 = [[
Longue chaîne du bloc 70, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue70 = [==[
Niveau 2 du bloc 70 : ]] et ]=] restent dans la chaîne.
]==]
  if bloc70.cle1 ~= nil then
    print('bloc 70', "total", bloc70.cle1)
  end

--[==[ Bloc 71 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc71 = {
  cle0 = 1705, -- hexa 0x6a9, flottant 243.571, exposant 1705e-2
  cle1 = 1708, -- hexa 0x6ac, flottant 244.000, exposant 1708e-2
  cle2 = 1711, -- hexa 0x6af, flottant 244.429, exposant 1711e-2
  cle3 = 1714, -- hexa 0x6b2, flottant 244.857, exposant 1714e-2
  cle4 = 1717, -- hexa 0x6b5, flottant 245.286, exposant 1717e-2
  cle5 = 1720, -- hexa 0x6b8, flottant 245.714, exposant 1720e-2
  cle6 = 1723, -- hexa 0x6bb, flottant 246.143, exposant 1723e-2
  cle7 = 1726, -- hexa 0x6be, flottant 246.571, exposant 1726e-2
}
function corpus.somme71(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte71 = [[
Longue chaîne du bloc 71, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 72 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc72 = {
  cle0 = 1729, -- hexa 0x6c1, flottant 247.000, exposant 1729e-2
  cle1 = 1732, -- hexa 0x6c4, flottant 247.429, exposant 1732e-2
  cle2 = 1735, -- hexa 0x6c7, flottant 247.857, exposant 1735e-2
  cle3 = 1738, -- hexa 0x6ca, flottant 248.286, exposant 1738e-2
  cle4 = 1741, -- hexa 0x6cd, flottant 248.714, exposant 1741e-2
  cle5 = 1744, -- hexa 0x6d0, flottant 249.143, exposant 1744e-2
  cle6 = 1747, -- hexa 0x6d3, flottant 249.571, exposant 1747e-2
  cle7 = 1750, -- hexa 0x6d6, flottant 250.000, exposant 1750e-2
}
function corpus.somme72(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte72 = [[
Longue chaîne du bloc 72, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 73 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc73 = {
  cle0 = 1753, -- hexa 0x6d9, flottant 250.429, exposant 1753e-2
  cle1 = 1756, -- hexa 0x6dc, flottant 250.857, exposant 1756e-2
  cle2 = 1759, -- hexa 0x6df, flottant 251.286, exposant 1759e-2
  cle3 = 1762, -- hexa 0x6e2, flottant 251.714, exposant 1762e-2
  cle4 = 1765, -- hexa 0x6e5, flottant 252.143, exposant 1765e-2
  cle5 = 1768, -- hexa 0x6e8, flottant 252.571, exposant 1768e-2
  cle6 = 1771, -- hexa 0x6eb, flottant 253.000, exposant 1771e-2
  cle7 = 1774, -- hexa 0x6ee, flottant 253.429, exposant 1774e-2
}
function corpus.somme73(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte73 = [[
Longue chaîne du bloc 73, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 74 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc74 = {
  cle0 = 1777, -- hexa 0x6f1, flottant 253.857, exposant 1777e-2
  cle1 = 1780, -- hexa 0x6f4, flottant 254.286, exposant 1780e-2
  cle2 = 1783, -- hexa 0x6f7, flottant 254.714, exposant 1783e-2
  cle3 = 1786, -- hexa 0x6fa, flottant 255.143, exposant 1786e-2
  cle4 = 1789, -- hexa 0x6fd, flottant 255.571, exposant 1789e-2
  cle5 = 1792, -- hexa 0x700, flottant 256.000, exposant 1792e-2
  cle6 = 1795, -- hexa 0x703, flottant 256.429, exposant 1795e-2
  cle7 = 1798, -- hexa 0x706, flottant 256.857, exposant 1798e-2
}
function corpus.somme74(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte74 = [[
Longue chaîne du bloc 74, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]

--[==[ Bloc 75 : longue commentaire à niveau 2.
    ]] et ]=] n'y ferment rien ; l'état traverse. ]==]
local bloc75 = {
  cle0 = 1801, -- hexa 0x709, flottant 257.286, exposant 1801e-2
  cle1 = 1804, -- hexa 0x70c, flottant 257.714, exposant 1804e-2
  cle2 = 1807, -- hexa 0x70f, flottant 258.143, exposant 1807e-2
  cle3 = 1810, -- hexa 0x712, flottant 258.571, exposant 1810e-2
  cle4 = 1813, -- hexa 0x715, flottant 259.000, exposant 1813e-2
  cle5 = 1816, -- hexa 0x718, flottant 259.429, exposant 1816e-2
  cle6 = 1819, -- hexa 0x71b, flottant 259.857, exposant 1819e-2
  cle7 = 1822, -- hexa 0x71e, flottant 260.286, exposant 1822e-2
}
function corpus.somme75(a, b)
  local total = (a or 0) + (b or 0)
  if total > 100 then
    total = total // 2
  else
    total = total & 0xFF
  end
  return total
end
local texte75 = [[
Longue chaîne du bloc 75, ligne une.
Ligne deux : \n reste écrit tel quel, sans fermeture.
]]
local longue75 = [==[
Niveau 2 du bloc 75 : ]] et ]=] restent dans la chaîne.
]==]

return corpus
