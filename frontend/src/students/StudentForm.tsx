import { parse } from 'date-fns'
import React, { useEffect, useMemo, useState } from 'react'

import { formatDate, parseDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import {
  GroupOfInputRows,
  LabeledInputFull,
  LabeledInputM,
  LabeledInputS,
  RowOfInputs
} from '../shared/layout'
import { Label } from '../shared/typography'

import { StudentDetails, StudentInput } from './api'

interface CreateProps {
  mode: 'CREATE'
  onChange: (validInput: StudentInput | null) => void
}
interface ViewProps {
  mode: 'VIEW'
  student: StudentDetails
}
interface EditProps {
  mode: 'EDIT'
  student: StudentDetails
  onChange: (validInput: StudentInput | null) => void
}
type Props = CreateProps | ViewProps | EditProps

export const StudentForm = React.memo(function StudentForm(props: Props) {
  const [valpasLink, setValpasLink] = useState(
    props.mode === 'CREATE' ? '' : props.student.valpasLink
  )
  const [ssn, setSsn] = useState(
    props.mode === 'CREATE' ? '' : props.student.ssn
  )
  const [firstName, setFirstName] = useState(
    props.mode === 'CREATE' ? '' : props.student.firstName
  )
  const [lastName, setLastName] = useState(
    props.mode === 'CREATE' ? '' : props.student.lastName
  )
  const [dateOfBirth, setDateOfBirth] = useState(
    props.mode === 'CREATE' || !props.student.dateOfBirth
      ? ''
      : formatDate(props.student.dateOfBirth)
  )
  const [phone, setPhone] = useState(
    props.mode === 'CREATE' ? '' : props.student.phone
  )
  const [email, setEmail] = useState(
    props.mode === 'CREATE' ? '' : props.student.email
  )
  const [address, setAdress] = useState(
    props.mode === 'CREATE' ? '' : props.student.address
  )

  useEffect(() => {
    if (ssn.length >= 6) {
      try {
        const dateFromSsn = parse(ssn, 'ddMMyy', new Date())
        setDateOfBirth(formatDate(dateFromSsn))
      } catch (e) {
        // ignore
      }
    }
  }, [ssn])

  const isValid =
    firstName.trim() !== '' &&
    lastName.trim() !== '' &&
    (dateOfBirth === '' || parseDate(dateOfBirth) !== undefined)

  const validInput: StudentInput | null = useMemo(
    () =>
      isValid
        ? {
            valpasLink: valpasLink.trim(),
            ssn: ssn.trim(),
            firstName: firstName.trim(),
            lastName: lastName.trim(),
            dateOfBirth: dateOfBirth.trim()
              ? parseDate(dateOfBirth.trim()) ?? null
              : null,
            phone: phone.trim(),
            email: email.trim(),
            address: address.trim()
          }
        : null,
    [
      isValid,
      valpasLink,
      ssn,
      firstName,
      lastName,
      dateOfBirth,
      phone,
      email,
      address
    ]
  )

  useEffect(() => {
    if (props.mode !== 'VIEW') {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <GroupOfInputRows $gapSize="L">
      <RowOfInputs $gapSize="m">
        <LabeledInputS>
          <Label>Hetu</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.student.ssn || '-'}</span>
          ) : (
            <InputField onChange={setSsn} value={ssn} />
          )}
        </LabeledInputS>
        <LabeledInputS>
          <Label>Syntymäaika</Label>
          {props.mode === 'VIEW' ? (
            <span>
              {props.student.dateOfBirth
                ? formatDate(props.student.dateOfBirth)
                : '-'}
            </span>
          ) : (
            <InputField onChange={setDateOfBirth} value={dateOfBirth} />
          )}
        </LabeledInputS>
        <LabeledInputFull>
          <Label>Valpas linkki</Label>
          {props.mode === 'VIEW' ? (
            props.student.valpasLink ? (
              <a
                href={props.student.valpasLink}
                target="_blank"
                rel="noreferrer"
              >
                {props.student.valpasLink}
              </a>
            ) : (
              <span>-</span>
            )
          ) : (
            <InputField onChange={setValpasLink} value={valpasLink} />
          )}
        </LabeledInputFull>
      </RowOfInputs>
      <RowOfInputs $gapSize="L">
        <LabeledInputM>
          <Label>Etunimi</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.student.firstName}</span>
          ) : (
            <InputField onChange={setFirstName} value={firstName} />
          )}
        </LabeledInputM>
        <LabeledInputM>
          <Label>Sukunimi</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.student.lastName}</span>
          ) : (
            <InputField onChange={setLastName} value={lastName} />
          )}
        </LabeledInputM>
      </RowOfInputs>
      <RowOfInputs $gapSize="L">
        <LabeledInputM>
          <Label>Puhelinnumero</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.student.phone || '-'}</span>
          ) : (
            <InputField onChange={setPhone} value={phone} />
          )}
        </LabeledInputM>
        <LabeledInputM>
          <Label>Sähköposti</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.student.email || '-'}</span>
          ) : (
            <InputField onChange={setEmail} value={email} />
          )}
        </LabeledInputM>
        <LabeledInputFull>
          <Label>Lähiosoite</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.student.address || '-'}</span>
          ) : (
            <InputField onChange={setAdress} value={address} />
          )}
        </LabeledInputFull>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})
