# SMS Gateway

Simple E-mail to SMS forwarding utility with a battery saver built-in. Best intended use would be simple SMS sending from PHP for example:

mail('sms@domain.tld', '0981122334', 'SMS message here');

Just setup the email account from which to forward the e-mails to SMS.

NOTICE: forwarded e-mails will be deleted.

RULES:

Phone number must be contained in the Subject: field or if you're using a catchall in the To: field.

Ordinary e-mail account:

From: whatever@wherever.tld<br>
To: configuredReceivingEmail@configuredTLD.tld<br>
Subject: 0981122334

will be forwarded to 0981122334

Catchall:

From: whoever@wherever.tld<br>
To: 0981122334@configuredTLD.tld

will be forwarded to 0981122334
