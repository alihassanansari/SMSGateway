# SMS Gateway

Simple E-mail to SMS forwarding utility with a battery saver builtin.

Just setup the email account from which to forward e-mails to SMS.

NOTICE: forwarded e-mails will be deleted.

RULES:

Phone number must be contained in the Subject: field or if you're using a catchall in the From: field.

Examples:

From: 0981122334@wherever.tld
To: configuredReceivingEmail@configuredTLD.tld

will be forwarded to 0981122334

From: whatever@wherever.tld
To: configuredReceivingEmail@configuredTLD.tld
Subject: 0981122334

will be forwarded to 0981122334
