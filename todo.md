# Session

La session devrait être en base de données, Jolt peut intégrer automatiquement une table session dans la base de données de docker. Mais il faudrait également laisser la possibilité aux développeur de configurer la table automatiquement avec les configurations de sessions dans la configuration de sécurité. Aussi, si l'utilisateur ne définit pas de base de données, il faudrait effectivement, utiliser les fichiers, notamment le tmp/tomcat pour stockées les session, Mais je devrait me renseigner car potentiellement, Jakarta EE ServeletSession qui doit certainement déjà avoir des shitsss et après nous ont vas faire un layer on-top de Jakarta EE ServeletSession.


# CSRF

# XSS

# Upload file

Pour l'upload de fichier, il ne faut pas utiliser le nom original car ont peut y injecter des code malicieux. Il faut donc utiliser un nom généré par le serveur. De manière cryptographique utilise alphanumeric.
Faire des recherches pour protéger les fichiers dans le code.

# Logging

Improve logging for security and different logging level.

# Freemarker templating

Update freemarker default configuration to always enforce escaping variables to automatically prevent XSS.


CloudOVH 

Créé des rapports de sécurité, grace à des outils tels que OWASP ZAP.