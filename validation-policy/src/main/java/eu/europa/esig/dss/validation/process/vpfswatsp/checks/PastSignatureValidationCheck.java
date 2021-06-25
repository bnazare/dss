/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.validation.process.vpfswatsp.checks;

import eu.europa.esig.dss.detailedreport.jaxb.XmlBlockType;
import eu.europa.esig.dss.detailedreport.jaxb.XmlPSV;
import eu.europa.esig.dss.detailedreport.jaxb.XmlValidationProcessArchivalData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.i18n.I18nProvider;
import eu.europa.esig.dss.i18n.MessageTag;
import eu.europa.esig.dss.policy.jaxb.LevelConstraint;

/**
 * Checks if the past signature validation result is acceptable
 */
public class PastSignatureValidationCheck extends AbstractPastTokenValidationCheck {

	/**
	 * Default constructor
	 *
	 * @param i18nProvider {@link I18nProvider}
	 * @param result {@link XmlValidationProcessArchivalData}
	 * @param signature {@link SignatureWrapper}
	 * @param xmlPSV {@link XmlPSV}
	 * @param constraint {@link LevelConstraint}
	 */
	public PastSignatureValidationCheck(I18nProvider i18nProvider, XmlValidationProcessArchivalData result,
										SignatureWrapper signature, XmlPSV xmlPSV, LevelConstraint constraint) {
		super(i18nProvider, result, signature, xmlPSV, constraint);
	}

	@Override
	protected XmlBlockType getBlockType() {
		return XmlBlockType.PSV;
	}

	@Override
	protected MessageTag getMessageTag() {
		return MessageTag.PSV_IPSVC;
	}

	@Override
	protected MessageTag getErrorMessageTag() {
		return MessageTag.PSV_IPSVC_ANS;
	}

}
