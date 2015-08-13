/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.validator.routines;

import java.io.Serializable;
import java.net.IDN;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p><b>Domain name</b> validation routines.</p>
 *
 * <p>
 * This validator provides methods for validating Internet domain names
 * and top-level domains.
 * </p>
 *
 * <p>Domain names are evaluated according
 * to the standards <a href="http://www.ietf.org/rfc/rfc1034.txt">RFC1034</a>,
 * section 3, and <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC1123</a>,
 * section 2.1. No accommodation is provided for the specialized needs of
 * other applications; if the domain name has been URL-encoded, for example,
 * validation will fail even though the equivalent plaintext version of the
 * same name would have passed.
 * </p>
 *
 * <p>
 * Validation is also provided for top-level domains (TLDs) as defined and
 * maintained by the Internet Assigned Numbers Authority (IANA):
 * </p>
 *
 *   <ul>
 *     <li>{@link #isValidInfrastructureTld} - validates infrastructure TLDs
 *         (<code>.arpa</code>, etc.)</li>
 *     <li>{@link #isValidGenericTld} - validates generic TLDs
 *         (<code>.com, .org</code>, etc.)</li>
 *     <li>{@link #isValidCountryCodeTld} - validates country code TLDs
 *         (<code>.us, .uk, .cn</code>, etc.)</li>
 *   </ul>
 *
 * <p>
 * (<b>NOTE</b>: This class does not provide IP address lookup for domain names or
 * methods to ensure that a given domain name matches a specific IP; see
 * {@link java.net.InetAddress} for that functionality.)
 * </p>
 *
 * @version $Revision$
 * @since Validator 1.4
 */
public class DomainValidator implements Serializable {

    private static final long serialVersionUID = -4407125112880174009L;

    // Regular expression strings for hostnames (derived from RFC2396 and RFC 1123)

    // RFC2396: domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
    // Max 63 characters
    private static final String DOMAIN_LABEL_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    // RFC2396 toplabel = alpha | alpha *( alphanum | "-" ) alphanum
    // Max 63 characters
    private static final String TOP_LABEL_REGEX = "\\p{Alpha}(?>[\\p{Alnum}-]{0,61}\\p{Alnum})?";

    // RFC2396 hostname = *( domainlabel "." ) toplabel [ "." ]
    // Note that the regex currently requires both a domain label and a top level label, whereas
    // the RFC does not. This is because the regex is used to detect if a TLD is present.
    // If the match fails, input is checked against DOMAIN_LABEL_REGEX (hostnameRegex)
    // RFC1123 sec 2.1 allows hostnames to start with a digit
    private static final String DOMAIN_NAME_REGEX =
            "^(?:" + DOMAIN_LABEL_REGEX + "\\.)+" + "(" + TOP_LABEL_REGEX + ")\\.?$";

    private final boolean allowLocal;

    /**
     * Singleton instance of this validator, which
     *  doesn't consider local addresses as valid.
     */
    private static final DomainValidator DOMAIN_VALIDATOR = new DomainValidator(false);

    /**
     * Singleton instance of this validator, which does
     *  consider local addresses valid.
     */
    private static final DomainValidator DOMAIN_VALIDATOR_WITH_LOCAL = new DomainValidator(true);

    /**
     * RegexValidator for matching domains.
     */
    private final RegexValidator domainRegex =
            new RegexValidator(DOMAIN_NAME_REGEX);
    /**
     * RegexValidator for matching a local hostname
     */
    // RFC1123 sec 2.1 allows hostnames to start with a digit
    private final RegexValidator hostnameRegex =
            new RegexValidator(DOMAIN_LABEL_REGEX);

    /**
     * Returns the singleton instance of this validator. It
     *  will not consider local addresses as valid.
     * @return the singleton instance of this validator
     */
    public static DomainValidator getInstance() {
        return DOMAIN_VALIDATOR;
    }

    /**
     * Returns the singleton instance of this validator,
     *  with local validation as required.
     * @param allowLocal Should local addresses be considered valid?
     * @return the singleton instance of this validator
     */
    public static DomainValidator getInstance(boolean allowLocal) {
       if(allowLocal) {
          return DOMAIN_VALIDATOR_WITH_LOCAL;
       }
       return DOMAIN_VALIDATOR;
    }

    /** Private constructor. */
    private DomainValidator(boolean allowLocal) {
       this.allowLocal = allowLocal;
    }

    /**
     * Returns true if the specified <code>String</code> parses
     * as a valid domain name with a recognized top-level domain.
     * The parsing is case-insensitive.
     * @param domain the parameter to check for domain name syntax
     * @return true if the parameter is a valid domain name
     */
    public boolean isValid(String domain) {
        if (domain == null) {
            return false;
        }
        domain = unicodeToASCII(domain);
        // hosts must be equally reachable via punycode and Unicode;
        // Unicode is never shorter than punycode, so check punycode
        // if domain did not convert, then it will be caught by ASCII
        // checks in the regexes below
        if (domain.length() > 253) {
            return false;
        }
        String[] groups = domainRegex.match(domain);
        if (groups != null && groups.length > 0) {
            return isValidTld(groups[0]);
        }
        return allowLocal && hostnameRegex.isValid(domain);
    }

    // package protected for unit test access
    // must agree with isValid() above
    final boolean isValidDomainSyntax(String domain) {
        if (domain == null) {
            return false;
        }
        domain = unicodeToASCII(domain);
        // hosts must be equally reachable via punycode and Unicode;
        // Unicode is never shorter than punycode, so check punycode
        // if domain did not convert, then it will be caught by ASCII
        // checks in the regexes below
        if (domain.length() > 253) {
            return false;
        }
        String[] groups = domainRegex.match(domain);
        return (groups != null && groups.length > 0)
                || hostnameRegex.isValid(domain);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined top-level domain. Leading dots are ignored if present.
     * The search is case-insensitive.
     * @param tld the parameter to check for TLD status, not null
     * @return true if the parameter is a TLD
     */
    public boolean isValidTld(String tld) {
        tld = unicodeToASCII(tld);
        if(allowLocal && isValidLocalTld(tld)) {
           return true;
        }
        return isValidInfrastructureTld(tld)
                || isValidGenericTld(tld)
                || isValidCountryCodeTld(tld);
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined infrastructure top-level domain. Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param iTld the parameter to check for infrastructure TLD status, not null
     * @return true if the parameter is an infrastructure TLD
     */
    public boolean isValidInfrastructureTld(String iTld) {
        iTld = unicodeToASCII(iTld);
        return Arrays.binarySearch(INFRASTRUCTURE_TLDS, (chompLeadingDot(iTld.toLowerCase(Locale.ENGLISH)))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined generic top-level domain. Leading dots are ignored
     * if present. The search is case-insensitive.
     * @param gTld the parameter to check for generic TLD status, not null
     * @return true if the parameter is a generic TLD
     */
    public boolean isValidGenericTld(String gTld) {
        gTld = unicodeToASCII(gTld);
        return Arrays.binarySearch(GENERIC_TLDS, chompLeadingDot(gTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * IANA-defined country code top-level domain. Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param ccTld the parameter to check for country code TLD status, not null
     * @return true if the parameter is a country code TLD
     */
    public boolean isValidCountryCodeTld(String ccTld) {
        ccTld = unicodeToASCII(ccTld);
        return Arrays.binarySearch(COUNTRY_CODE_TLDS, chompLeadingDot(ccTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    /**
     * Returns true if the specified <code>String</code> matches any
     * widely used "local" domains (localhost or localdomain). Leading dots are
     * ignored if present. The search is case-insensitive.
     * @param lTld the parameter to check for local TLD status, not null
     * @return true if the parameter is an local TLD
     */
    public boolean isValidLocalTld(String lTld) {
        lTld = unicodeToASCII(lTld);
        return Arrays.binarySearch(LOCAL_TLDS, chompLeadingDot(lTld.toLowerCase(Locale.ENGLISH))) >= 0;
    }

    private String chompLeadingDot(String str) {
        if (str.startsWith(".")) {
            return str.substring(1);
        }
        return str;
    }

    // ---------------------------------------------
    // ----- TLDs defined by IANA
    // ----- Authoritative and comprehensive list at:
    // ----- http://data.iana.org/TLD/tlds-alpha-by-domain.txt

    // Note that the above list is in UPPER case.
    // The code currently converts strings to lower case (as per the tables below)

    // IANA also provide an HTML list at http://www.iana.org/domains/root/db
    // Note that this contains several country code entries which are NOT in
    // the text file. These all have the "Not assigned" in the "Sponsoring Organisation" column
    // For example (as of 2015-01-02):
    // .bl  country-code    Not assigned
    // .um  country-code    Not assigned

    // WARNING: this array MUST be sorted, others it cannot be searched reliably using binary search
    private static final String[] INFRASTRUCTURE_TLDS = new String[] {
        "arpa",               // internet infrastructure
    };

    // WARNING: this array MUST be sorted, others it cannot be searched reliably using binary search
    private static final String[] GENERIC_TLDS = new String[] {
        // Taken from Version 2015081201, Last Updated Thu Aug 13 07:07:01 2015 UTC
        "abb", // abb ABB Ltd
        "abbott", // abbott Abbott Laboratories, Inc.
        "abogado", // abogado Top Level Domain Holdings Limited
        "academy", // academy Half Oaks, LLC
        "accenture", // accenture Accenture plc
        "accountant", // accountant dot Accountant Limited
        "accountants", // accountants Knob Town, LLC
        "active", // active The Active Network, Inc
        "actor", // actor United TLD Holdco Ltd.
        "ads", // ads Charleston Road Registry Inc.
        "adult", // adult ICM Registry AD LLC
        "aeg", // aeg Aktiebolaget Electrolux
        "aero", // aero Societe Internationale de Telecommunications Aeronautique (SITA INC USA)
        "afl", // afl Australian Football League
        "agency", // agency Steel Falls, LLC
        "aig", // aig American International Group, Inc.
        "airforce", // airforce United TLD Holdco Ltd.
        "airtel", // airtel Bharti Airtel Limited
        "allfinanz", // allfinanz Allfinanz Deutsche Vermögensberatung Aktiengesellschaft
        "alsace", // alsace REGION D ALSACE
        "amsterdam", // amsterdam Gemeente Amsterdam
        "android", // android Charleston Road Registry Inc.
        "apartments", // apartments June Maple, LLC
        "app", // app Charleston Road Registry Inc.
        "aquarelle", // aquarelle Aquarelle.com
        "archi", // archi STARTING DOT LIMITED
        "army", // army United TLD Holdco Ltd.
        "asia", // asia DotAsia Organisation Ltd.
        "associates", // associates Baxter Hill, LLC
        "attorney", // attorney United TLD Holdco, Ltd
        "auction", // auction United TLD HoldCo, Ltd.
        "audio", // audio Uniregistry, Corp.
        "auto", // auto Uniregistry, Corp.
        "autos", // autos DERAutos, LLC
        "axa", // axa AXA SA
        "azure", // azure Microsoft Corporation
        "band", // band United TLD Holdco, Ltd
        "bank", // bank fTLD Registry Services, LLC
        "bar", // bar Punto 2012 Sociedad Anonima Promotora de Inversion de Capital Variable
        "barcelona", // barcelona Municipi de Barcelona
        "barclaycard", // barclaycard Barclays Bank PLC
        "barclays", // barclays Barclays Bank PLC
        "bargains", // bargains Half Hallow, LLC
        "bauhaus", // bauhaus Werkhaus GmbH
        "bayern", // bayern Bayern Connect GmbH
        "bbc", // bbc British Broadcasting Corporation
        "bbva", // bbva BANCO BILBAO VIZCAYA ARGENTARIA, S.A.
        "bcn", // bcn Municipi de Barcelona
        "beer", // beer Top Level Domain Holdings Limited
        "bentley", // bentley Bentley Motors Limited
        "berlin", // berlin dotBERLIN GmbH &amp; Co. KG
        "best", // best BestTLD Pty Ltd
        "bet", // bet Afilias plc
        "bharti", // bharti Bharti Enterprises (Holding) Private Limited
        "bible", // bible American Bible Society
        "bid", // bid dot Bid Limited
        "bike", // bike Grand Hollow, LLC
        "bing", // bing Microsoft Corporation
        "bingo", // bingo Sand Cedar, LLC
        "bio", // bio STARTING DOT LIMITED
        "biz", // biz Neustar, Inc.
        "black", // black Afilias Limited
        "blackfriday", // blackfriday Uniregistry, Corp.
        "bloomberg", // bloomberg Bloomberg IP Holdings LLC
        "blue", // blue Afilias Limited
        "bmw", // bmw Bayerische Motoren Werke Aktiengesellschaft
        "bnl", // bnl Banca Nazionale del Lavoro
        "bnpparibas", // bnpparibas BNP Paribas
        "boats", // boats DERBoats, LLC
        "bond", // bond Bond University Limited
        "boo", // boo Charleston Road Registry Inc.
        "boots", // boots THE BOOTS COMPANY PLC
        "boutique", // boutique Over Galley, LLC
        "bradesco", // bradesco Banco Bradesco S.A.
        "bridgestone", // bridgestone Bridgestone Corporation
        "broker", // broker DOTBROKER REGISTRY LTD
        "brother", // brother Brother Industries, Ltd.
        "brussels", // brussels DNS.be vzw
        "budapest", // budapest Top Level Domain Holdings Limited
        "build", // build Plan Bee LLC
        "builders", // builders Atomic Madison, LLC
        "business", // business Spring Cross, LLC
        "buzz", // buzz DOTSTRATEGY CO.
        "bzh", // bzh Association www.bzh
        "cab", // cab Half Sunset, LLC
        "cafe", // cafe Pioneer Canyon, LLC
        "cal", // cal Charleston Road Registry Inc.
        "camera", // camera Atomic Maple, LLC
        "camp", // camp Delta Dynamite, LLC
        "cancerresearch", // cancerresearch Australian Cancer Research Foundation
        "canon", // canon Canon Inc.
        "capetown", // capetown ZA Central Registry NPC trading as ZA Central Registry
        "capital", // capital Delta Mill, LLC
        "caravan", // caravan Caravan International, Inc.
        "cards", // cards Foggy Hollow, LLC
        "care", // care Goose Cross, LLC
        "career", // career dotCareer LLC
        "careers", // careers Wild Corner, LLC
        "cars", // cars Uniregistry, Corp.
        "cartier", // cartier Richemont DNS Inc.
        "casa", // casa Top Level Domain Holdings Limited
        "cash", // cash Delta Lake, LLC
        "casino", // casino Binky Sky, LLC
        "cat", // cat Fundacio puntCAT
        "catering", // catering New Falls. LLC
        "cba", // cba COMMONWEALTH BANK OF AUSTRALIA
        "cbn", // cbn The Christian Broadcasting Network, Inc.
        "ceb", // ceb The Corporate Executive Board Company
        "center", // center Tin Mill, LLC
        "ceo", // ceo CEOTLD Pty Ltd
        "cern", // cern European Organization for Nuclear Research (&quot;CERN&quot;)
        "cfa", // cfa CFA Institute
        "cfd", // cfd DOTCFD REGISTRY LTD
        "chanel", // chanel Chanel International B.V.
        "channel", // channel Charleston Road Registry Inc.
        "chat", // chat Sand Fields, LLC
        "cheap", // cheap Sand Cover, LLC
        "chloe", // chloe Richemont DNS Inc.
        "christmas", // christmas Uniregistry, Corp.
        "chrome", // chrome Charleston Road Registry Inc.
        "church", // church Holly Fileds, LLC
        "cisco", // cisco Cisco Technology, Inc.
        "citic", // citic CITIC Group Corporation
        "city", // city Snow Sky, LLC
        "claims", // claims Black Corner, LLC
        "cleaning", // cleaning Fox Shadow, LLC
        "click", // click Uniregistry, Corp.
        "clinic", // clinic Goose Park, LLC
        "clothing", // clothing Steel Lake, LLC
        "cloud", // cloud ARUBA S.p.A.
        "club", // club .CLUB DOMAINS, LLC
        "coach", // coach Koko Island, LLC
        "codes", // codes Puff Willow, LLC
        "coffee", // coffee Trixy Cover, LLC
        "college", // college XYZ.COM LLC
        "cologne", // cologne NetCologne Gesellschaft für Telekommunikation mbH
        "com", // com VeriSign Global Registry Services
        "commbank", // commbank COMMONWEALTH BANK OF AUSTRALIA
        "community", // community Fox Orchard, LLC
        "company", // company Silver Avenue, LLC
        "computer", // computer Pine Mill, LLC
        "condos", // condos Pine House, LLC
        "construction", // construction Fox Dynamite, LLC
        "consulting", // consulting United TLD Holdco, LTD.
        "contractors", // contractors Magic Woods, LLC
        "cooking", // cooking Top Level Domain Holdings Limited
        "cool", // cool Koko Lake, LLC
        "coop", // coop DotCooperation LLC
        "corsica", // corsica Collectivité Territoriale de Corse
        "country", // country Top Level Domain Holdings Limited
        "coupons", // coupons Black Island, LLC
        "courses", // courses OPEN UNIVERSITIES AUSTRALIA PTY LTD
        "credit", // credit Snow Shadow, LLC
        "creditcard", // creditcard Binky Frostbite, LLC
        "cricket", // cricket dot Cricket Limited
        "crown", // crown Crown Equipment Corporation
        "crs", // crs Federated Co-operatives Limited
        "cruises", // cruises Spring Way, LLC
        "cuisinella", // cuisinella SALM S.A.S.
        "cymru", // cymru Nominet UK
        "cyou", // cyou Beijing Gamease Age Digital Technology Co., Ltd.
        "dabur", // dabur Dabur India Limited
        "dad", // dad Charleston Road Registry Inc.
        "dance", // dance United TLD Holdco Ltd.
        "date", // date dot Date Limited
        "dating", // dating Pine Fest, LLC
        "datsun", // datsun NISSAN MOTOR CO., LTD.
        "day", // day Charleston Road Registry Inc.
        "dclk", // dclk Charleston Road Registry Inc.
        "deals", // deals Sand Sunset, LLC
        "degree", // degree United TLD Holdco, Ltd
        "delivery", // delivery Steel Station, LLC
        "delta", // delta Delta Air Lines, Inc.
        "democrat", // democrat United TLD Holdco Ltd.
        "dental", // dental Tin Birch, LLC
        "dentist", // dentist United TLD Holdco, Ltd
        "desi", // desi Desi Networks LLC
        "design", // design Top Level Design, LLC
        "dev", // dev Charleston Road Registry Inc.
        "diamonds", // diamonds John Edge, LLC
        "diet", // diet Uniregistry, Corp.
        "digital", // digital Dash Park, LLC
        "direct", // direct Half Trail, LLC
        "directory", // directory Extra Madison, LLC
        "discount", // discount Holly Hill, LLC
        "dnp", // dnp Dai Nippon Printing Co., Ltd.
        "docs", // docs Charleston Road Registry Inc.
        "dog", // dog Koko Mill, LLC
        "doha", // doha Communications Regulatory Authority (CRA)
        "domains", // domains Sugar Cross, LLC
        "doosan", // doosan Doosan Corporation
        "download", // download dot Support Limited
        "drive", // drive Charleston Road Registry Inc.
        "durban", // durban ZA Central Registry NPC trading as ZA Central Registry
        "dvag", // dvag Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "earth", // earth Interlink Co., Ltd.
        "eat", // eat Charleston Road Registry Inc.
        "edu", // edu EDUCAUSE
        "education", // education Brice Way, LLC
        "email", // email Spring Madison, LLC
        "emerck", // emerck Merck KGaA
        "energy", // energy Binky Birch, LLC
        "engineer", // engineer United TLD Holdco Ltd.
        "engineering", // engineering Romeo Canyon
        "enterprises", // enterprises Snow Oaks, LLC
        "epson", // epson Seiko Epson Corporation
        "equipment", // equipment Corn Station, LLC
        "erni", // erni ERNI Group Holding AG
        "esq", // esq Charleston Road Registry Inc.
        "estate", // estate Trixy Park, LLC
        "eurovision", // eurovision European Broadcasting Union (EBU)
        "eus", // eus Puntueus Fundazioa
        "events", // events Pioneer Maple, LLC
        "everbank", // everbank EverBank
        "exchange", // exchange Spring Falls, LLC
        "expert", // expert Magic Pass, LLC
        "exposed", // exposed Victor Beach, LLC
        "express", // express Sea Sunset, LLC
        "fage", // fage Fage International S.A.
        "fail", // fail Atomic Pipe, LLC
        "faith", // faith dot Faith Limited
        "family", // family United TLD Holdco Ltd.
        "fan", // fan Asiamix Digital Ltd
        "fans", // fans Asiamix Digital Limited
        "farm", // farm Just Maple, LLC
        "fashion", // fashion Top Level Domain Holdings Limited
        "feedback", // feedback Top Level Spectrum, Inc.
        "film", // film Motion Picture Domain Registry Pty Ltd
        "finance", // finance Cotton Cypress, LLC
        "financial", // financial Just Cover, LLC
        "firmdale", // firmdale Firmdale Holdings Limited
        "fish", // fish Fox Woods, LLC
        "fishing", // fishing Top Level Domain Holdings Limited
        "fit", // fit Minds + Machines Group Limited
        "fitness", // fitness Brice Orchard, LLC
        "flights", // flights Fox Station, LLC
        "florist", // florist Half Cypress, LLC
        "flowers", // flowers Uniregistry, Corp.
        "flsmidth", // flsmidth FLSmidth A/S
        "fly", // fly Charleston Road Registry Inc.
        "foo", // foo Charleston Road Registry Inc.
        "football", // football Foggy Farms, LLC
        "forex", // forex DOTFOREX REGISTRY LTD
        "forsale", // forsale United TLD Holdco, LLC
        "forum", // forum Fegistry, LLC
        "foundation", // foundation John Dale, LLC
        "frl", // frl FRLregistry B.V.
        "frogans", // frogans OP3FT
        "fund", // fund John Castle, LLC
        "furniture", // furniture Lone Fields, LLC
        "futbol", // futbol United TLD Holdco, Ltd.
        "fyi", // fyi Silver Tigers, LLC
        "gal", // gal Asociación puntoGAL
        "gallery", // gallery Sugar House, LLC
        "game", // game Uniregistry, Corp.
        "garden", // garden Top Level Domain Holdings Limited
        "gbiz", // gbiz Charleston Road Registry Inc.
        "gdn", // gdn Joint Stock Company &quot;Navigation-information systems&quot;
        "gent", // gent COMBELL GROUP NV/SA
        "genting", // genting Resorts World Inc. Pte. Ltd.
        "ggee", // ggee GMO Internet, Inc.
        "gift", // gift Uniregistry, Corp.
        "gifts", // gifts Goose Sky, LLC
        "gives", // gives United TLD Holdco Ltd.
        "giving", // giving Giving Limited
        "glass", // glass Black Cover, LLC
        "gle", // gle Charleston Road Registry Inc.
        "global", // global Dot Global Domain Registry Limited
        "globo", // globo Globo Comunicação e Participações S.A
        "gmail", // gmail Charleston Road Registry Inc.
        "gmo", // gmo GMO Internet, Inc.
        "gmx", // gmx 1&amp;1 Mail &amp; Media GmbH
        "gold", // gold June Edge, LLC
        "goldpoint", // goldpoint YODOBASHI CAMERA CO.,LTD.
        "golf", // golf Lone Falls, LLC
        "goo", // goo NTT Resonant Inc.
        "goog", // goog Charleston Road Registry Inc.
        "google", // google Charleston Road Registry Inc.
        "gop", // gop Republican State Leadership Committee, Inc.
        "gov", // gov General Services Administration Attn: QTDC, 2E08 (.gov Domain Registration)
        "graphics", // graphics Over Madison, LLC
        "gratis", // gratis Pioneer Tigers, LLC
        "green", // green Afilias Limited
        "gripe", // gripe Corn Sunset, LLC
        "group", // group Romeo Town, LLC
        "guge", // guge Charleston Road Registry Inc.
        "guide", // guide Snow Moon, LLC
        "guitars", // guitars Uniregistry, Corp.
        "guru", // guru Pioneer Cypress, LLC
        "hamburg", // hamburg Hamburg Top-Level-Domain GmbH
        "hangout", // hangout Charleston Road Registry Inc.
        "haus", // haus United TLD Holdco, LTD.
        "healthcare", // healthcare Silver Glen, LLC
        "help", // help Uniregistry, Corp.
        "here", // here Charleston Road Registry Inc.
        "hermes", // hermes Hermes International
        "hiphop", // hiphop Uniregistry, Corp.
        "hitachi", // hitachi Hitachi, Ltd.
        "hiv", // hiv dotHIV gemeinnuetziger e.V.
        "hockey", // hockey Half Willow, LLC
        "holdings", // holdings John Madison, LLC
        "holiday", // holiday Goose Woods, LLC
        "homedepot", // homedepot Homer TLC, Inc.
        "homes", // homes DERHomes, LLC
        "honda", // honda Honda Motor Co., Ltd.
        "horse", // horse Top Level Domain Holdings Limited
        "host", // host DotHost Inc.
        "hosting", // hosting Uniregistry, Corp.
        "hoteles", // hoteles Travel Reservations SRL
        "hotmail", // hotmail Microsoft Corporation
        "house", // house Sugar Park, LLC
        "how", // how Charleston Road Registry Inc.
        "hsbc", // hsbc HSBC Holdings PLC
        "ibm", // ibm International Business Machines Corporation
        "icbc", // icbc Industrial and Commercial Bank of China Limited
        "ice", // ice IntercontinentalExchange, Inc.
        "icu", // icu One.com A/S
        "ifm", // ifm ifm electronic gmbh
        "iinet", // iinet Connect West Pty. Ltd.
        "immo", // immo Auburn Bloom, LLC
        "immobilien", // immobilien United TLD Holdco Ltd.
        "industries", // industries Outer House, LLC
        "infiniti", // infiniti NISSAN MOTOR CO., LTD.
        "info", // info Afilias Limited
        "ing", // ing Charleston Road Registry Inc.
        "ink", // ink Top Level Design, LLC
        "institute", // institute Outer Maple, LLC
        "insure", // insure Pioneer Willow, LLC
        "int", // int Internet Assigned Numbers Authority
        "international", // international Wild Way, LLC
        "investments", // investments Holly Glen, LLC
        "ipiranga", // ipiranga Ipiranga Produtos de Petroleo S.A.
        "irish", // irish Dot-Irish LLC
        "ist", // ist Istanbul Metropolitan Municipality
        "istanbul", // istanbul Istanbul Metropolitan Municipality / Medya A.S.
        "itau", // itau Itau Unibanco Holding S.A.
        "iwc", // iwc Richemont DNS Inc.
        "java", // java Oracle Corporation
        "jcb", // jcb JCB Co., Ltd.
        "jetzt", // jetzt New TLD Company AB
        "jewelry", // jewelry Wild Bloom, LLC
        "jlc", // jlc Richemont DNS Inc.
        "jll", // jll Jones Lang LaSalle Incorporated
        "jobs", // jobs Employ Media LLC
        "joburg", // joburg ZA Central Registry NPC trading as ZA Central Registry
        "jprs", // jprs Japan Registry Services Co., Ltd.
        "juegos", // juegos Uniregistry, Corp.
        "kaufen", // kaufen United TLD Holdco Ltd.
        "kddi", // kddi KDDI CORPORATION
        "kim", // kim Afilias Limited
        "kitchen", // kitchen Just Goodbye, LLC
        "kiwi", // kiwi DOT KIWI LIMITED
        "koeln", // koeln NetCologne Gesellschaft für Telekommunikation mbH
        "komatsu", // komatsu Komatsu Ltd.
        "krd", // krd KRG Department of Information Technology
        "kred", // kred KredTLD Pty Ltd
        "kyoto", // kyoto Academic Institution: Kyoto Jyoho Gakuen
        "lacaixa", // lacaixa CAIXA D&#39;ESTALVIS I PENSIONS DE BARCELONA
        "lancaster", // lancaster LANCASTER
        "land", // land Pine Moon, LLC
        "lasalle", // lasalle Jones Lang LaSalle Incorporated
        "lat", // lat ECOM-LAC Federación de Latinoamérica y el Caribe para Internet y el Comercio Electrónico
        "latrobe", // latrobe La Trobe University
        "law", // law Minds + Machines Group Limited
        "lawyer", // lawyer United TLD Holdco, Ltd
        "lds", // lds IRI Domain Management, LLC
        "lease", // lease Victor Trail, LLC
        "leclerc", // leclerc A.C.D. LEC Association des Centres Distributeurs Edouard Leclerc
        "legal", // legal Blue Falls, LLC
        "lexus", // lexus TOYOTA MOTOR CORPORATION
        "lgbt", // lgbt Afilias Limited
        "liaison", // liaison Liaison Technologies, Incorporated
        "lidl", // lidl Schwarz Domains und Services GmbH &amp; Co. KG
        "life", // life Trixy Oaks, LLC
        "lighting", // lighting John McCook, LLC
        "limited", // limited Big Fest, LLC
        "limo", // limo Hidden Frostbite, LLC
        "link", // link Uniregistry, Corp.
        "live", // live United TLD Holdco Ltd.
        "lixil", // lixil LIXIL Group Corporation
        "loan", // loan dot Loan Limited
        "loans", // loans June Woods, LLC
        "lol", // lol Uniregistry, Corp.
        "london", // london Dot London Domains Limited
        "lotte", // lotte Lotte Holdings Co., Ltd.
        "lotto", // lotto Afilias Limited
        "love", // love Merchant Law Group LLP
        "ltda", // ltda InterNetX Corp.
        "lupin", // lupin LUPIN LIMITED
        "luxe", // luxe Top Level Domain Holdings Limited
        "luxury", // luxury Luxury Partners LLC
        "madrid", // madrid Comunidad de Madrid
        "maif", // maif Mutuelle Assurance Instituteur France (MAIF)
        "maison", // maison Victor Frostbite, LLC
        "man", // man MAN SE
        "management", // management John Goodbye, LLC
        "mango", // mango PUNTO FA S.L.
        "market", // market Unitied TLD Holdco, Ltd
        "marketing", // marketing Fern Pass, LLC
        "markets", // markets DOTMARKETS REGISTRY LTD
        "marriott", // marriott Marriott Worldwide Corporation
        "mba", // mba Lone Hollow, LLC
        "media", // media Grand Glen, LLC
        "meet", // meet Afilias Limited
        "melbourne", // melbourne The Crown in right of the State of Victoria, represented by its Department of State Development, Business and Innovation
        "meme", // meme Charleston Road Registry Inc.
        "memorial", // memorial Dog Beach, LLC
        "men", // men Exclusive Registry Limited
        "menu", // menu Wedding TLD2, LLC
        "miami", // miami Top Level Domain Holdings Limited
        "microsoft", // microsoft Microsoft Corporation
        "mil", // mil DoD Network Information Center
        "mini", // mini Bayerische Motoren Werke Aktiengesellschaft
        "mma", // mma MMA IARD
        "mobi", // mobi Afilias Technologies Limited dba dotMobi
        "moda", // moda United TLD Holdco Ltd.
        "moe", // moe Interlink Co., Ltd.
        "monash", // monash Monash University
        "money", // money Outer McCook, LLC
        "montblanc", // montblanc Richemont DNS Inc.
        "mormon", // mormon IRI Domain Management, LLC (&quot;Applicant&quot;)
        "mortgage", // mortgage United TLD Holdco, Ltd
        "moscow", // moscow Foundation for Assistance for Internet Technologies and Infrastructure Development (FAITID)
        "motorcycles", // motorcycles DERMotorcycles, LLC
        "mov", // mov Charleston Road Registry Inc.
        "movie", // movie New Frostbite, LLC
        "movistar", // movistar Telefónica S.A.
        "mtn", // mtn MTN Dubai Limited
        "mtpc", // mtpc Mitsubishi Tanabe Pharma Corporation
        "museum", // museum Museum Domain Management Association
        "nadex", // nadex Nadex Domains, Inc
        "nagoya", // nagoya GMO Registry, Inc.
        "name", // name VeriSign Information Services, Inc.
        "navy", // navy United TLD Holdco Ltd.
        "nec", // nec NEC Corporation
        "net", // net VeriSign Global Registry Services
        "netbank", // netbank COMMONWEALTH BANK OF AUSTRALIA
        "network", // network Trixy Manor, LLC
        "neustar", // neustar NeuStar, Inc.
        "new", // new Charleston Road Registry Inc.
        "news", // news United TLD Holdco Ltd.
        "nexus", // nexus Charleston Road Registry Inc.
        "ngo", // ngo Public Interest Registry
        "nhk", // nhk Japan Broadcasting Corporation (NHK)
        "nico", // nico DWANGO Co., Ltd.
        "ninja", // ninja United TLD Holdco Ltd.
        "nissan", // nissan NISSAN MOTOR CO., LTD.
        "nokia", // nokia Nokia Corporation
        "nra", // nra NRA Holdings Company, INC.
        "nrw", // nrw Minds + Machines GmbH
        "ntt", // ntt NIPPON TELEGRAPH AND TELEPHONE CORPORATION
        "nyc", // nyc The City of New York by and through the New York City Department of Information Technology &amp; Telecommunications
        "office", // office Microsoft Corporation
        "okinawa", // okinawa BusinessRalliart inc.
        "omega", // omega The Swatch Group Ltd
        "one", // one One.com A/S
        "ong", // ong Public Interest Registry
        "onl", // onl I-REGISTRY Ltd., Niederlassung Deutschland
        "online", // online DotOnline Inc.
        "ooo", // ooo INFIBEAM INCORPORATION LIMITED
        "oracle", // oracle Oracle Corporation
        "orange", // orange Orange Brand Services Limited
        "org", // org Public Interest Registry (PIR)
        "organic", // organic Afilias Limited
        "osaka", // osaka Interlink Co., Ltd.
        "otsuka", // otsuka Otsuka Holdings Co., Ltd.
        "ovh", // ovh OVH SAS
        "page", // page Charleston Road Registry Inc.
        "panerai", // panerai Richemont DNS Inc.
        "paris", // paris City of Paris
        "partners", // partners Magic Glen, LLC
        "parts", // parts Sea Goodbye, LLC
        "party", // party Blue Sky Registry Limited
        "pet", // pet Afilias plc
        "pharmacy", // pharmacy National Association of Boards of Pharmacy
        "philips", // philips Koninklijke Philips N.V.
        "photo", // photo Uniregistry, Corp.
        "photography", // photography Sugar Glen, LLC
        "photos", // photos Sea Corner, LLC
        "physio", // physio PhysBiz Pty Ltd
        "piaget", // piaget Richemont DNS Inc.
        "pics", // pics Uniregistry, Corp.
        "pictet", // pictet Pictet Europe S.A.
        "pictures", // pictures Foggy Sky, LLC
        "pink", // pink Afilias Limited
        "pizza", // pizza Foggy Moon, LLC
        "place", // place Snow Galley, LLC
        "play", // play Charleston Road Registry Inc.
        "plumbing", // plumbing Spring Tigers, LLC
        "plus", // plus Sugar Mill, LLC
        "pohl", // pohl Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "poker", // poker Afilias Domains No. 5 Limited
        "porn", // porn ICM Registry PN LLC
        "post", // post Universal Postal Union
        "praxi", // praxi Praxi S.p.A.
        "press", // press DotPress Inc.
        "pro", // pro Registry Services Corporation dba RegistryPro
        "prod", // prod Charleston Road Registry Inc.
        "productions", // productions Magic Birch, LLC
        "prof", // prof Charleston Road Registry Inc.
        "properties", // properties Big Pass, LLC
        "property", // property Uniregistry, Corp.
        "pub", // pub United TLD Holdco Ltd.
        "qpon", // qpon dotCOOL, Inc.
        "quebec", // quebec PointQuébec Inc
        "racing", // racing Premier Registry Limited
        "realtor", // realtor Real Estate Domains LLC
        "realty", // realty Fegistry, LLC
        "recipes", // recipes Grand Island, LLC
        "red", // red Afilias Limited
        "redstone", // redstone Redstone Haute Couture Co., Ltd.
        "rehab", // rehab United TLD Holdco Ltd.
        "reise", // reise Foggy Way, LLC
        "reisen", // reisen New Cypress, LLC
        "reit", // reit National Association of Real Estate Investment Trusts, Inc.
        "ren", // ren Beijing Qianxiang Wangjing Technology Development Co., Ltd.
        "rent", // rent XYZ.COM LLC
        "rentals", // rentals Big Hollow,LLC
        "repair", // repair Lone Sunset, LLC
        "report", // report Binky Glen, LLC
        "republican", // republican United TLD Holdco Ltd.
        "rest", // rest Punto 2012 Sociedad Anonima Promotora de Inversion de Capital Variable
        "restaurant", // restaurant Snow Avenue, LLC
        "review", // review dot Review Limited
        "reviews", // reviews United TLD Holdco, Ltd.
        "rich", // rich I-REGISTRY Ltd., Niederlassung Deutschland
        "ricoh", // ricoh Ricoh Company, Ltd.
        "rio", // rio Empresa Municipal de Informática SA - IPLANRIO
        "rip", // rip United TLD Holdco Ltd.
        "rocks", // rocks United TLD Holdco, LTD.
        "rodeo", // rodeo Top Level Domain Holdings Limited
        "rsvp", // rsvp Charleston Road Registry Inc.
        "ruhr", // ruhr regiodot GmbH &amp; Co. KG
        "run", // run Snow Park, LLC
        "ryukyu", // ryukyu BusinessRalliart inc.
        "saarland", // saarland dotSaarland GmbH
        "sakura", // sakura SAKURA Internet Inc.
        "sale", // sale United TLD Holdco, Ltd
        "samsung", // samsung SAMSUNG SDS CO., LTD
        "sandvik", // sandvik Sandvik AB
        "sandvikcoromant", // sandvikcoromant Sandvik AB
        "sanofi", // sanofi Sanofi
        "sap", // sap SAP AG
        "sarl", // sarl Delta Orchard, LLC
        "saxo", // saxo Saxo Bank A/S
        "sca", // sca SVENSKA CELLULOSA AKTIEBOLAGET SCA (publ)
        "scb", // scb The Siam Commercial Bank Public Company Limited (&quot;SCB&quot;)
        "schmidt", // schmidt SALM S.A.S.
        "scholarships", // scholarships Scholarships.com, LLC
        "school", // school Little Galley, LLC
        "schule", // schule Outer Moon, LLC
        "schwarz", // schwarz Schwarz Domains und Services GmbH &amp; Co. KG
        "science", // science dot Science Limited
        "scor", // scor SCOR SE
        "scot", // scot Dot Scot Registry Limited
        "seat", // seat SEAT, S.A. (Sociedad Unipersonal)
        "seek", // seek Seek Limited
        "sener", // sener Sener Ingeniería y Sistemas, S.A.
        "services", // services Fox Castle, LLC
        "sew", // sew SEW-EURODRIVE GmbH &amp; Co KG
        "sex", // sex ICM Registry SX LLC
        "sexy", // sexy Uniregistry, Corp.
        "shiksha", // shiksha Afilias Limited
        "shoes", // shoes Binky Galley, LLC
        "show", // show Snow Beach, LLC
        "shriram", // shriram Shriram Capital Ltd.
        "singles", // singles Fern Madison, LLC
        "site", // site DotSite Inc.
        "ski", // ski STARTING DOT LIMITED
        "sky", // sky Sky International AG
        "skype", // skype Microsoft Corporation
        "sncf", // sncf SNCF (Société Nationale des Chemins de fer Francais)
        "soccer", // soccer Foggy Shadow, LLC
        "social", // social United TLD Holdco Ltd.
        "software", // software United TLD Holdco, Ltd
        "sohu", // sohu Sohu.com Limited
        "solar", // solar Ruby Town, LLC
        "solutions", // solutions Silver Cover, LLC
        "sony", // sony Sony Corporation
        "soy", // soy Charleston Road Registry Inc.
        "space", // space DotSpace Inc.
        "spiegel", // spiegel SPIEGEL-Verlag Rudolf Augstein GmbH &amp; Co. KG
        "spreadbetting", // spreadbetting DOTSPREADBETTING REGISTRY LTD
        "srl", // srl InterNetX Corp.
        "starhub", // starhub StarHub Limited
        "statoil", // statoil Statoil ASA
        "studio", // studio United TLD Holdco Ltd.
        "study", // study OPEN UNIVERSITIES AUSTRALIA PTY LTD
        "style", // style Binky Moon, LLC
        "sucks", // sucks Vox Populi Registry Ltd.
        "supplies", // supplies Atomic Fields, LLC
        "supply", // supply Half Falls, LLC
        "support", // support Grand Orchard, LLC
        "surf", // surf Top Level Domain Holdings Limited
        "surgery", // surgery Tin Avenue, LLC
        "suzuki", // suzuki SUZUKI MOTOR CORPORATION
        "swatch", // swatch The Swatch Group Ltd
        "swiss", // swiss Swiss Confederation
        "sydney", // sydney State of New South Wales, Department of Premier and Cabinet
        "systems", // systems Dash Cypress, LLC
        "taipei", // taipei Taipei City Government
        "tatamotors", // tatamotors Tata Motors Ltd
        "tatar", // tatar Limited Liability Company &quot;Coordination Center of Regional Domain of Tatarstan Republic&quot;
        "tattoo", // tattoo Uniregistry, Corp.
        "tax", // tax Storm Orchard, LLC
        "taxi", // taxi Pine Falls, LLC
        "team", // team Atomic Lake, LLC
        "tech", // tech Dot Tech LLC
        "technology", // technology Auburn Falls, LLC
        "tel", // tel Telnic Ltd.
        "telefonica", // telefonica Telefónica S.A.
        "temasek", // temasek Temasek Holdings (Private) Limited
        "tennis", // tennis Cotton Bloom, LLC
        "thd", // thd Homer TLC, Inc.
        "theater", // theater Blue Tigers, LLC
        "tickets", // tickets Accent Media Limited
        "tienda", // tienda Victor Manor, LLC
        "tips", // tips Corn Willow, LLC
        "tires", // tires Dog Edge, LLC
        "tirol", // tirol punkt Tirol GmbH
        "today", // today Pearl Woods, LLC
        "tokyo", // tokyo GMO Registry, Inc.
        "tools", // tools Pioneer North, LLC
        "top", // top Jiangsu Bangning Science &amp; Technology Co.,Ltd.
        "toray", // toray Toray Industries, Inc.
        "toshiba", // toshiba TOSHIBA Corporation
        "tours", // tours Sugar Station, LLC
        "town", // town Koko Moon, LLC
        "toyota", // toyota TOYOTA MOTOR CORPORATION
        "toys", // toys Pioneer Orchard, LLC
        "trade", // trade Elite Registry Limited
        "trading", // trading DOTTRADING REGISTRY LTD
        "training", // training Wild Willow, LLC
        "travel", // travel Tralliance Registry Management Company, LLC.
        "trust", // trust Artemis Internet Inc
        "tui", // tui TUI AG
        "ubs", // ubs UBS AG
        "university", // university Little Station, LLC
        "uno", // uno Dot Latin LLC
        "uol", // uol UBN INTERNET LTDA.
        "vacations", // vacations Atomic Tigers, LLC
        "vegas", // vegas Dot Vegas, Inc.
        "ventures", // ventures Binky Lake, LLC
        "versicherung", // versicherung dotversicherung-registry GmbH
        "vet", // vet United TLD Holdco, Ltd
        "viajes", // viajes Black Madison, LLC
        "video", // video United TLD Holdco, Ltd
        "villas", // villas New Sky, LLC
        "vin", // vin Holly Shadow, LLC
        "vision", // vision Koko Station, LLC
        "vista", // vista Vistaprint Limited
        "vistaprint", // vistaprint Vistaprint Limited
        "vlaanderen", // vlaanderen DNS.be vzw
        "vodka", // vodka Top Level Domain Holdings Limited
        "vote", // vote Monolith Registry LLC
        "voting", // voting Valuetainment Corp.
        "voto", // voto Monolith Registry LLC
        "voyage", // voyage Ruby House, LLC
        "wales", // wales Nominet UK
        "walter", // walter Sandvik AB
        "wang", // wang Zodiac Registry Limited
        "watch", // watch Sand Shadow, LLC
        "webcam", // webcam dot Webcam Limited
        "website", // website DotWebsite Inc.
        "wed", // wed Atgron, Inc.
        "wedding", // wedding Top Level Domain Holdings Limited
        "weir", // weir Weir Group IP Limited
        "whoswho", // whoswho Who&#39;s Who Registry
        "wien", // wien punkt.wien GmbH
        "wiki", // wiki Top Level Design, LLC
        "williamhill", // williamhill William Hill Organization Limited
        "win", // win First Registry Limited
        "windows", // windows Microsoft Corporation
        "wine", // wine June Station, LLC
        "wme", // wme William Morris Endeavor Entertainment, LLC
        "work", // work Top Level Domain Holdings Limited
        "works", // works Little Dynamite, LLC
        "world", // world Bitter Fields, LLC
        "wtc", // wtc World Trade Centers Association, Inc.
        "wtf", // wtf Hidden Way, LLC
        "xbox", // xbox Microsoft Corporation
        "xerox", // xerox Xerox DNHC LLC
        "xin", // xin Elegant Leader Limited
        "xn--11b4c3d", // कॉम VeriSign Sarl
        "xn--1qqw23a", // 佛山 Guangzhou YU Wei Information Technology Co., Ltd.
        "xn--30rr7y", // 慈善 Excellent First Limited
        "xn--3bst00m", // 集团 Eagle Horizon Limited
        "xn--3ds443g", // 在线 TLD REGISTRY LIMITED
        "xn--3pxu8k", // 点看 VeriSign Sarl
        "xn--42c2d9a", // คอม VeriSign Sarl
        "xn--45q11c", // 八卦 Zodiac Scorpio Limited
        "xn--4gbrim", // موقع Suhub Electronic Establishment
        "xn--55qw42g", // 公益 China Organizational Name Administration Center
        "xn--55qx5d", // 公司 Computer Network Information Center of Chinese Academy of Sciences （China Internet Network Information Center）
        "xn--6frz82g", // 移动 Afilias Limited
        "xn--6qq986b3xl", // 我爱你 Tycoon Treasure Limited
        "xn--80adxhks", // москва Foundation for Assistance for Internet Technologies and Infrastructure Development (FAITID)
        "xn--80asehdb", // онлайн CORE Association
        "xn--80aswg", // сайт CORE Association
        "xn--9dbq2a", // קום VeriSign Sarl
        "xn--9et52u", // 时尚 RISE VICTORY LIMITED
        "xn--b4w605ferd", // 淡马锡 Temasek Holdings (Private) Limited
        "xn--c1avg", // орг Public Interest Registry
        "xn--c2br7g", // नेट VeriSign Sarl
        "xn--cg4bki", // 삼성 SAMSUNG SDS CO., LTD
        "xn--czr694b", // 商标 HU YI GLOBAL INFORMATION RESOURCES(HOLDING) COMPANY.HONGKONG LIMITED
        "xn--czrs0t", // 商店 Wild Island, LLC
        "xn--czru2d", // 商城 Zodiac Aquarius Limited
        "xn--d1acj3b", // дети The Foundation for Network Initiatives “The Smart Internet”
        "xn--estv75g", // 工行 Industrial and Commercial Bank of China Limited
        "xn--fhbei", // كوم VeriSign Sarl
        "xn--fiq228c5hs", // 中文网 TLD REGISTRY LIMITED
        "xn--fiq64b", // 中信 CITIC Group Corporation
        "xn--fjq720a", // 娱乐 Will Bloom, LLC
        "xn--flw351e", // 谷歌 Charleston Road Registry Inc.
        "xn--hxt814e", // 网店 Zodiac Libra Limited
        "xn--i1b6b1a6a2e", // संगठन Public Interest Registry
        "xn--imr513n", // 餐厅 HU YI GLOBAL INFORMATION RESOURCES (HOLDING) COMPANY. HONGKONG LIMITED
        "xn--io0a7i", // 网络 Computer Network Information Center of Chinese Academy of Sciences （China Internet Network Information Center）
        "xn--j1aef", // ком VeriSign Sarl
        "xn--kcrx77d1x4a", // 飞利浦 Koninklijke Philips N.V.
        "xn--kput3i", // 手机 Beijing RITT-Net Technology Development Co., Ltd
        "xn--mgbab2bd", // بازار CORE Association
        "xn--mk1bu44c", // 닷컴 VeriSign Sarl
        "xn--mxtq1m", // 政府 Net-Chinese Co., Ltd.
        "xn--ngbc5azd", // شبكة International Domain Registry Pty. Ltd.
        "xn--nqv7f", // 机构 Public Interest Registry
        "xn--nqv7fs00ema", // 组织机构 Public Interest Registry
        "xn--nyqy26a", // 健康 Stable Tone Limited
        "xn--p1acf", // рус Rusnames Limited
        "xn--pssy2u", // 大拿 VeriSign Sarl
        "xn--q9jyb4c", // みんな Charleston Road Registry Inc.
        "xn--qcka1pmc", // グーグル Charleston Road Registry Inc.
        "xn--rhqv96g", // 世界 Stable Tone Limited
        "xn--ses554g", // 网址 KNET Co., Ltd
        "xn--t60b56a", // 닷넷 VeriSign Sarl
        "xn--tckwe", // コム VeriSign Sarl
        "xn--unup4y", // 游戏 Spring Fields, LLC
        "xn--vermgensberater-ctb", // VERMöGENSBERATER Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "xn--vermgensberatung-pwb", // VERMöGENSBERATUNG Deutsche Vermögensberatung Aktiengesellschaft DVAG
        "xn--vhquv", // 企业 Dash McCook, LLC
        "xn--vuq861b", // 信息 Beijing Tele-info Network Technology Co., Ltd.
        "xn--xhq521b", // 广东 Guangzhou YU Wei Information Technology Co., Ltd.
        "xn--zfr164b", // 政务 China Organizational Name Administration Center
        "xperia", // xperia Sony Mobile Communications AB
        "xxx", // xxx ICM Registry LLC
        "xyz", // xyz XYZ.COM LLC
        "yachts", // yachts DERYachts, LLC
        "yandex", // yandex YANDEX, LLC
        "yodobashi", // yodobashi YODOBASHI CAMERA CO.,LTD.
        "yoga", // yoga Top Level Domain Holdings Limited
        "yokohama", // yokohama GMO Registry, Inc.
        "youtube", // youtube Charleston Road Registry Inc.
        "zip", // zip Charleston Road Registry Inc.
        "zone", // zone Outer Falls, LLC
        "zuerich", // zuerich Kanton Zürich (Canton of Zurich)
    };

    // WARNING: this array MUST be sorted, others it cannot be searched reliably using binary search
    private static final String[] COUNTRY_CODE_TLDS = new String[] {
        "ac",                 // Ascension Island
        "ad",                 // Andorra
        "ae",                 // United Arab Emirates
        "af",                 // Afghanistan
        "ag",                 // Antigua and Barbuda
        "ai",                 // Anguilla
        "al",                 // Albania
        "am",                 // Armenia
//        "an",                 // Netherlands Antilles (retired)
        "ao",                 // Angola
        "aq",                 // Antarctica
        "ar",                 // Argentina
        "as",                 // American Samoa
        "at",                 // Austria
        "au",                 // Australia (includes Ashmore and Cartier Islands and Coral Sea Islands)
        "aw",                 // Aruba
        "ax",                 // Åland
        "az",                 // Azerbaijan
        "ba",                 // Bosnia and Herzegovina
        "bb",                 // Barbados
        "bd",                 // Bangladesh
        "be",                 // Belgium
        "bf",                 // Burkina Faso
        "bg",                 // Bulgaria
        "bh",                 // Bahrain
        "bi",                 // Burundi
        "bj",                 // Benin
        "bm",                 // Bermuda
        "bn",                 // Brunei Darussalam
        "bo",                 // Bolivia
        "br",                 // Brazil
        "bs",                 // Bahamas
        "bt",                 // Bhutan
        "bv",                 // Bouvet Island
        "bw",                 // Botswana
        "by",                 // Belarus
        "bz",                 // Belize
        "ca",                 // Canada
        "cc",                 // Cocos (Keeling) Islands
        "cd",                 // Democratic Republic of the Congo (formerly Zaire)
        "cf",                 // Central African Republic
        "cg",                 // Republic of the Congo
        "ch",                 // Switzerland
        "ci",                 // Côte d'Ivoire
        "ck",                 // Cook Islands
        "cl",                 // Chile
        "cm",                 // Cameroon
        "cn",                 // China, mainland
        "co",                 // Colombia
        "cr",                 // Costa Rica
        "cu",                 // Cuba
        "cv",                 // Cape Verde
        "cw",                 // Curaçao
        "cx",                 // Christmas Island
        "cy",                 // Cyprus
        "cz",                 // Czech Republic
        "de",                 // Germany
        "dj",                 // Djibouti
        "dk",                 // Denmark
        "dm",                 // Dominica
        "do",                 // Dominican Republic
        "dz",                 // Algeria
        "ec",                 // Ecuador
        "ee",                 // Estonia
        "eg",                 // Egypt
        "er",                 // Eritrea
        "es",                 // Spain
        "et",                 // Ethiopia
        "eu",                 // European Union
        "fi",                 // Finland
        "fj",                 // Fiji
        "fk",                 // Falkland Islands
        "fm",                 // Federated States of Micronesia
        "fo",                 // Faroe Islands
        "fr",                 // France
        "ga",                 // Gabon
        "gb",                 // Great Britain (United Kingdom)
        "gd",                 // Grenada
        "ge",                 // Georgia
        "gf",                 // French Guiana
        "gg",                 // Guernsey
        "gh",                 // Ghana
        "gi",                 // Gibraltar
        "gl",                 // Greenland
        "gm",                 // The Gambia
        "gn",                 // Guinea
        "gp",                 // Guadeloupe
        "gq",                 // Equatorial Guinea
        "gr",                 // Greece
        "gs",                 // South Georgia and the South Sandwich Islands
        "gt",                 // Guatemala
        "gu",                 // Guam
        "gw",                 // Guinea-Bissau
        "gy",                 // Guyana
        "hk",                 // Hong Kong
        "hm",                 // Heard Island and McDonald Islands
        "hn",                 // Honduras
        "hr",                 // Croatia (Hrvatska)
        "ht",                 // Haiti
        "hu",                 // Hungary
        "id",                 // Indonesia
        "ie",                 // Ireland (Éire)
        "il",                 // Israel
        "im",                 // Isle of Man
        "in",                 // India
        "io",                 // British Indian Ocean Territory
        "iq",                 // Iraq
        "ir",                 // Iran
        "is",                 // Iceland
        "it",                 // Italy
        "je",                 // Jersey
        "jm",                 // Jamaica
        "jo",                 // Jordan
        "jp",                 // Japan
        "ke",                 // Kenya
        "kg",                 // Kyrgyzstan
        "kh",                 // Cambodia (Khmer)
        "ki",                 // Kiribati
        "km",                 // Comoros
        "kn",                 // Saint Kitts and Nevis
        "kp",                 // North Korea
        "kr",                 // South Korea
        "kw",                 // Kuwait
        "ky",                 // Cayman Islands
        "kz",                 // Kazakhstan
        "la",                 // Laos (currently being marketed as the official domain for Los Angeles)
        "lb",                 // Lebanon
        "lc",                 // Saint Lucia
        "li",                 // Liechtenstein
        "lk",                 // Sri Lanka
        "lr",                 // Liberia
        "ls",                 // Lesotho
        "lt",                 // Lithuania
        "lu",                 // Luxembourg
        "lv",                 // Latvia
        "ly",                 // Libya
        "ma",                 // Morocco
        "mc",                 // Monaco
        "md",                 // Moldova
        "me",                 // Montenegro
        "mg",                 // Madagascar
        "mh",                 // Marshall Islands
        "mk",                 // Republic of Macedonia
        "ml",                 // Mali
        "mm",                 // Myanmar
        "mn",                 // Mongolia
        "mo",                 // Macau
        "mp",                 // Northern Mariana Islands
        "mq",                 // Martinique
        "mr",                 // Mauritania
        "ms",                 // Montserrat
        "mt",                 // Malta
        "mu",                 // Mauritius
        "mv",                 // Maldives
        "mw",                 // Malawi
        "mx",                 // Mexico
        "my",                 // Malaysia
        "mz",                 // Mozambique
        "na",                 // Namibia
        "nc",                 // New Caledonia
        "ne",                 // Niger
        "nf",                 // Norfolk Island
        "ng",                 // Nigeria
        "ni",                 // Nicaragua
        "nl",                 // Netherlands
        "no",                 // Norway
        "np",                 // Nepal
        "nr",                 // Nauru
        "nu",                 // Niue
        "nz",                 // New Zealand
        "om",                 // Oman
        "pa",                 // Panama
        "pe",                 // Peru
        "pf",                 // French Polynesia With Clipperton Island
        "pg",                 // Papua New Guinea
        "ph",                 // Philippines
        "pk",                 // Pakistan
        "pl",                 // Poland
        "pm",                 // Saint-Pierre and Miquelon
        "pn",                 // Pitcairn Islands
        "pr",                 // Puerto Rico
        "ps",                 // Palestinian territories (PA-controlled West Bank and Gaza Strip)
        "pt",                 // Portugal
        "pw",                 // Palau
        "py",                 // Paraguay
        "qa",                 // Qatar
        "re",                 // Réunion
        "ro",                 // Romania
        "rs",                 // Serbia
        "ru",                 // Russia
        "rw",                 // Rwanda
        "sa",                 // Saudi Arabia
        "sb",                 // Solomon Islands
        "sc",                 // Seychelles
        "sd",                 // Sudan
        "se",                 // Sweden
        "sg",                 // Singapore
        "sh",                 // Saint Helena
        "si",                 // Slovenia
        "sj",                 // Svalbard and Jan Mayen Islands Not in use (Norwegian dependencies; see .no)
        "sk",                 // Slovakia
        "sl",                 // Sierra Leone
        "sm",                 // San Marino
        "sn",                 // Senegal
        "so",                 // Somalia
        "sr",                 // Suriname
        "st",                 // São Tomé and Príncipe
        "su",                 // Soviet Union (deprecated)
        "sv",                 // El Salvador
        "sx",                 // Sint Maarten
        "sy",                 // Syria
        "sz",                 // Swaziland
        "tc",                 // Turks and Caicos Islands
        "td",                 // Chad
        "tf",                 // French Southern and Antarctic Lands
        "tg",                 // Togo
        "th",                 // Thailand
        "tj",                 // Tajikistan
        "tk",                 // Tokelau
        "tl",                 // East Timor (deprecated old code)
        "tm",                 // Turkmenistan
        "tn",                 // Tunisia
        "to",                 // Tonga
//        "tp",                 // East Timor (Retired)
        "tr",                 // Turkey
        "tt",                 // Trinidad and Tobago
        "tv",                 // Tuvalu
        "tw",                 // Taiwan, Republic of China
        "tz",                 // Tanzania
        "ua",                 // Ukraine
        "ug",                 // Uganda
        "uk",                 // United Kingdom
        "us",                 // United States of America
        "uy",                 // Uruguay
        "uz",                 // Uzbekistan
        "va",                 // Vatican City State
        "vc",                 // Saint Vincent and the Grenadines
        "ve",                 // Venezuela
        "vg",                 // British Virgin Islands
        "vi",                 // U.S. Virgin Islands
        "vn",                 // Vietnam
        "vu",                 // Vanuatu
        "wf",                 // Wallis and Futuna
        "ws",                 // Samoa (formerly Western Samoa)
        "xn--3e0b707e", // 한국 KISA (Korea Internet &amp; Security Agency)
        "xn--45brj9c", // ভারত National Internet Exchange of India
        "xn--80ao21a", // қаз Association of IT Companies of Kazakhstan
        "xn--90a3ac", // срб Serbian National Internet Domain Registry (RNIDS)
        "xn--90ais", // ??? Reliable Software Inc.
        "xn--clchc0ea0b2g2a9gcd", // சிங்கப்பூர் Singapore Network Information Centre (SGNIC) Pte Ltd
        "xn--d1alf", // мкд Macedonian Academic Research Network Skopje
        "xn--fiqs8s", // 中国 China Internet Network Information Center
        "xn--fiqz9s", // 中國 China Internet Network Information Center
        "xn--fpcrj9c3d", // భారత్ National Internet Exchange of India
        "xn--fzc2c9e2c", // ලංකා LK Domain Registry
        "xn--gecrj9c", // ભારત National Internet Exchange of India
        "xn--h2brj9c", // भारत National Internet Exchange of India
        "xn--j1amh", // укр Ukrainian Network Information Centre (UANIC), Inc.
        "xn--j6w193g", // 香港 Hong Kong Internet Registration Corporation Ltd.
        "xn--kprw13d", // 台湾 Taiwan Network Information Center (TWNIC)
        "xn--kpry57d", // 台灣 Taiwan Network Information Center (TWNIC)
        "xn--l1acc", // мон Datacom Co.,Ltd
        "xn--lgbbat1ad8j", // الجزائر CERIST
        "xn--mgb9awbf", // عمان Telecommunications Regulatory Authority (TRA)
        "xn--mgba3a4f16a", // ایران Institute for Research in Fundamental Sciences (IPM)
        "xn--mgbaam7a8h", // امارات Telecommunications Regulatory Authority (TRA)
        "xn--mgbayh7gpa", // الاردن National Information Technology Center (NITC)
        "xn--mgbbh1a71e", // بھارت National Internet Exchange of India
        "xn--mgbc0a9azcg", // المغرب Agence Nationale de Réglementation des Télécommunications (ANRT)
        "xn--mgberp4a5d4ar", // السعودية Communications and Information Technology Commission
        "xn--mgbpl2fh", // ????? Sudan Internet Society
        "xn--mgbx4cd0ab", // مليسيا MYNIC Berhad
        "xn--node", // გე Information Technologies Development Center (ITDC)
        "xn--o3cw4h", // ไทย Thai Network Information Center Foundation
        "xn--ogbpf8fl", // سورية National Agency for Network Services (NANS)
        "xn--p1ai", // рф Coordination Center for TLD RU
        "xn--pgbs0dh", // تونس Agence Tunisienne d&#39;Internet
        "xn--s9brj9c", // ਭਾਰਤ National Internet Exchange of India
        "xn--wgbh1c", // مصر National Telecommunication Regulatory Authority - NTRA
        "xn--wgbl6a", // قطر Communications Regulatory Authority
        "xn--xkc2al3hye2a", // இலங்கை LK Domain Registry
        "xn--xkc2dl3a5ee0h", // இந்தியா National Internet Exchange of India
        "xn--y9a3aq", // ??? Internet Society
        "xn--yfro4i67o", // 新加坡 Singapore Network Information Centre (SGNIC) Pte Ltd
        "xn--ygbi2ammx", // فلسطين Ministry of Telecom &amp; Information Technology (MTIT)
        "ye",                 // Yemen
        "yt",                 // Mayotte
        "za",                 // South Africa
        "zm",                 // Zambia
        "zw",                 // Zimbabwe
    };

    // WARNING: this array MUST be sorted, others it cannot be searched reliably using binary search
    private static final String[] LOCAL_TLDS = new String[] {
       "localdomain",         // Also widely used as localhost.localdomain
       "localhost",           // RFC2606 defined
    };

    /**
     * Converts potentially Unicode input to punycode.
     * If conversion fails, returns the original input.
     * 
     * @param input the string to convert, not null
     * @return converted input, or original input if conversion fails
     */
    // Needed by UrlValidator
    static String unicodeToASCII(String input) {
        if (isOnlyASCII(input)) { // skip possibly expensive processing
            return input;
        }
        try {
            final String ascii = IDN.toASCII(input);
            if (IDNBUGHOLDER.IDN_TOASCII_PRESERVES_TRAILING_DOTS) {
                return ascii;
            }
            final int length = input.length();
            if (length == 0) {// check there is a last character
                return input;
            }
// RFC3490 3.1. 1)
//            Whenever dots are used as label separators, the following
//            characters MUST be recognized as dots: U+002E (full stop), U+3002
//            (ideographic full stop), U+FF0E (fullwidth full stop), U+FF61
//            (halfwidth ideographic full stop).
            char lastChar = input.charAt(length-1);// fetch original last char
            switch(lastChar) {
                case '\u002E': // "." full stop
                case '\u3002': // ideographic full stop
                case '\uFF0E': // fullwidth full stop
                case '\uFF61': // halfwidth ideographic full stop
                    return ascii + "."; // restore the missing stop
                default:
                    return ascii;
            }
        } catch (IllegalArgumentException e) { // input is not valid
            return input;
        }
    }

    private static class IDNBUGHOLDER {
        private static boolean keepsTrailingDot() {
            final String input = "a."; // must be a valid name
            return input.equals(IDN.toASCII(input));
        }
        private static final boolean IDN_TOASCII_PRESERVES_TRAILING_DOTS = keepsTrailingDot();
    }

    /*
     * Check if input contains only ASCII
     * Treats null as all ASCII
     */
    private static boolean isOnlyASCII(String input) {
        if (input == null) {
            return true;
        }
        for(int i=0; i < input.length(); i++) {
            if (input.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }
}
