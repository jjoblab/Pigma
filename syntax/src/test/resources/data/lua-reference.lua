-- Compteur de caisse : un module Lua de démonstration.
-- La ligne qui suit ouvre une longue commentaire à deux niveaux.
--[==[
  La longue commentaire à niveau 2 : « ]] » et « ]=] » n'y ferment rien.
  Elle traverse les lignes, l'état du lexeur la transporte.
]==]

local caisse = {}

local taux = 0x10        -- 16 en hexadécimal
local marge = 3.5e-2     -- exposant décimal
local seuil = 1e3
local hexa = 0x1p4       -- flottant hexadécimal (16.0)
local part = .25         -- point initial

--- Additionne une valeur au compteur, borne le dépassement.
function caisse.ajouter(valeur)
  caisse.total = (caisse.total or 0) + valeur
  if caisse.total > seuil then
    caisse.total = seuil
  end
  return caisse.total
end

-- Retire une valeur si possible ; sinon rien.
function caisse.retirer(valeur)
  if not caisse.total or caisse.total < valeur then
    return nil, "solde insuffisant"
  end
  caisse.total = caisse.total - valeur
  return caisse.total
end

local function etiqueter(nom, valeur)
  return string.format("%s=%q", nom, valeur)
end

-- Chaîne longue de niveau zéro : le saut de ligne initial est ignoré,
-- aucune échappe n'y vit.
local notice = [[
Première ligne de la notice.
Deuxième ligne, « \n » reste écrit tel quel.]]

-- Chaîne longue de niveau un : « ]] » n'y ferme pas.
local parenthese = [=[
Un crochet simple [ dedans], la fermeture demandera ]=] le bon niveau.]=]

-- Boucle de contrôle avec bit à bit et division entière.
for i = 1, 10 do
  local pair = i // 2
  local bas = i & 0x3
  local haut = i >> 1
  if i % 2 ~= 0 then
    goto continue
  end
  print(etiqueter("pair", pair), 'valeur ' .. tostring(pair) .. " !")
  ::continue::
end

-- Tableau de chaînes : guillemets simples et doubles.
local mots = { 'un', "deux", "l'apostrophe", 'dit "bonjour"' }

-- Sélecteur de fin : répète jusqu'à la condition.
repeat
  caisse.ajouter(marge)
until caisse.total >= 1.0

-- Le corps du module : la table servie.
return setmetatable(caisse, {
  __index = function(table, cle)
    return "manquant : " .. tostring(cle)
  end,
  __tostring = function(table)
    return "caisse(" .. tostring(table.total) .. ")"
  end,
})
