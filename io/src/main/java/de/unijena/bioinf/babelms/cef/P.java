
package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="rt" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="rte" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="rts" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="rtw" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="s" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="v" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="x" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="y" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "p")
public class P {

    @XmlAttribute(name = "rt")
    protected BigDecimal rt;
    @XmlAttribute(name = "rte")
    protected BigDecimal rte;
    @XmlAttribute(name = "rts")
    protected BigDecimal rts;
    @XmlAttribute(name = "rtw")
    protected BigInteger rtw;
    @XmlAttribute(name = "s")
    @XmlSchemaType(name = "anySimpleType")
    protected String s;
    @XmlAttribute(name = "v")
    protected BigInteger v;
    @XmlAttribute(name = "x")
    protected BigDecimal x;
    @XmlAttribute(name = "y")
    protected BigDecimal y;
    @XmlAttribute(name = "z")
    protected BigInteger z;

    /**
     * Gets the value of the rt property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRt() {
        return rt;
    }

    /**
     * Sets the value of the rt property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRt(BigDecimal value) {
        this.rt = value;
    }

    /**
     * Gets the value of the rte property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRte() {
        return rte;
    }

    /**
     * Sets the value of the rte property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRte(BigDecimal value) {
        this.rte = value;
    }

    /**
     * Gets the value of the rts property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRts() {
        return rts;
    }

    /**
     * Sets the value of the rts property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRts(BigDecimal value) {
        this.rts = value;
    }

    /**
     * Gets the value of the rtw property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRtw() {
        return rtw;
    }

    /**
     * Sets the value of the rtw property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRtw(BigInteger value) {
        this.rtw = value;
    }

    /**
     * Gets the value of the s property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getS() {
        return s;
    }

    /**
     * Sets the value of the s property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setS(String value) {
        this.s = value;
    }

    /**
     * Gets the value of the v property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getV() {
        return v;
    }

    /**
     * Sets the value of the v property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setV(BigInteger value) {
        this.v = value;
    }

    /**
     * Gets the value of the x property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getX() {
        return x;
    }

    /**
     * Sets the value of the x property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setX(BigDecimal value) {
        this.x = value;
    }

    /**
     * Gets the value of the y property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setY(BigDecimal value) {
        this.y = value;
    }

    /**
     * Gets the value of the z property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getZ() {
        return z;
    }

    /**
     * Sets the value of the z property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setZ(BigInteger value) {
        this.z = value;
    }

}
