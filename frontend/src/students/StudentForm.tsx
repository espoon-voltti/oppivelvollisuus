import { parse } from 'date-fns'
import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { AlertBox, InfoBox } from '../shared/MessageBoxes'
import { formatDate, parseDate } from '../shared/dates'
import { Checkbox } from '../shared/form/Checkbox'
import { InputField } from '../shared/form/InputField'
import { Select } from '../shared/form/Select'
import { ReadOnlyTextArea, TextArea } from '../shared/form/TextArea'
import {
  FlexCol,
  FlexColWithGaps,
  FlexRowWithGaps,
  GroupOfInputRows,
  LabeledInput,
  RowOfInputs,
  VerticalGap
} from '../shared/layout'
import { H3, Label } from '../shared/typography'
import { useDebouncedState } from '../shared/useDebouncedState'

import {
  apiGetPossibleDuplicateStudents,
  DuplicateStudent,
  StudentDetails,
  StudentInput
} from './api'
import { Gender, genderNames, genders } from './enums'

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

const commonLanguages = [
  'suomi',
  'ruotsi',
  'englanti',
  'viro',
  'venäjä',
  'ukraina',
  'arabia',
  'somalia'
]

export const StudentForm = React.memo(function StudentForm(props: Props) {
  const duplicateCheckDelay = 1500
  const [ssn, setSsn, debouncedSsn] = useDebouncedState(
    props.mode === 'CREATE' ? '' : props.student.ssn,
    duplicateCheckDelay
  )
  const [dateOfBirth, setDateOfBirth] = useState(
    props.mode === 'CREATE' || !props.student.dateOfBirth
      ? ''
      : formatDate(props.student.dateOfBirth)
  )
  const [valpasLink, setValpasLink, debouncedValpasLink] = useDebouncedState(
    props.mode === 'CREATE' ? '' : props.student.valpasLink,
    duplicateCheckDelay
  )
  const [firstName, setFirstName, debouncedFirstName] = useDebouncedState(
    props.mode === 'CREATE' ? '' : props.student.firstName,
    duplicateCheckDelay
  )
  const [lastName, setLastName, debouncedLastName] = useDebouncedState(
    props.mode === 'CREATE' ? '' : props.student.lastName,
    duplicateCheckDelay
  )
  const [language, setLanguage] = useState(
    props.mode === 'CREATE' ? '' : props.student.language
  )
  const [phone, setPhone] = useState(
    props.mode === 'CREATE' ? '' : props.student.phone
  )
  const [email, setEmail] = useState(
    props.mode === 'CREATE' ? '' : props.student.email
  )
  const [gender, setGender] = useState(
    props.mode === 'CREATE' ? null : props.student.gender
  )
  const [address, setAddress] = useState(
    props.mode === 'CREATE' ? '' : props.student.address
  )
  const [municipalityInFinland, setMunicipalityInFinland] = useState(
    props.mode === 'CREATE' ? true : props.student.municipalityInFinland
  )
  const [guardianInfo, setGuardianInfo] = useState(
    props.mode === 'CREATE' ? '' : props.student.guardianInfo
  )
  const [supportContactsInfo, setSupportContactsInfo] = useState(
    props.mode === 'CREATE' ? '' : props.student.supportContactsInfo
  )

  const [duplicateStudents, setDuplicateStudents] = useState<
    DuplicateStudent[]
  >([])

  useEffect(() => {
    if (ssn.length >= 6) {
      try {
        const dateFromSsn = parse(ssn.substring(0, 6), 'ddMMyy', new Date())
        setDateOfBirth(formatDate(dateFromSsn))
      } catch (e) {
        // ignore
      }
    }
  }, [ssn])

  const creating = props.mode === 'CREATE'
  useEffect(() => {
    if (!creating) return

    void apiGetPossibleDuplicateStudents({
      ssn: debouncedSsn,
      valpasLink: debouncedValpasLink,
      firstName: debouncedFirstName,
      lastName: debouncedLastName
    }).then(setDuplicateStudents)
  }, [
    debouncedSsn,
    debouncedValpasLink,
    debouncedFirstName,
    debouncedLastName,
    creating
  ])

  const duplicateSsnStudent = duplicateStudents.find((s) => s.matchingSsn)
  const duplicateValpasStudent = duplicateStudents.find(
    (s) => s.matchingValpasLink
  )
  const duplicateNameStudents = duplicateStudents.filter((s) => s.matchingName)

  const validInput: StudentInput | null = useMemo(() => {
    if (firstName.trim() === '') return null
    if (lastName.trim() === '') return null
    const parsedDateOfBirth = parseDate(dateOfBirth.trim())
    if (!parsedDateOfBirth) return null

    return {
      valpasLink: valpasLink.trim(),
      ssn: ssn.trim(),
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      language: language.toLowerCase().trim(),
      dateOfBirth: parsedDateOfBirth,
      phone: phone.trim(),
      email: email.trim(),
      gender,
      address: address.trim(),
      municipalityInFinland,
      guardianInfo,
      supportContactsInfo
    }
  }, [
    valpasLink,
    ssn,
    firstName,
    lastName,
    language,
    dateOfBirth,
    phone,
    email,
    gender,
    address,
    municipalityInFinland,
    guardianInfo,
    supportContactsInfo
  ])

  useEffect(() => {
    if (props.mode !== 'VIEW') {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <FlexCol>
      <GroupOfInputRows>
        <RowOfInputs>
          <LabeledInput $cols={2}>
            <Label>Hetu</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.ssn || '-'}</span>
            ) : (
              <InputField onChange={setSsn} value={ssn} />
            )}
          </LabeledInput>
          <LabeledInput $cols={2}>
            <Label>Syntymäaika {props.mode !== 'VIEW' && '*'}</Label>
            {props.mode === 'VIEW' ? (
              <span>
                {props.student.dateOfBirth
                  ? formatDate(props.student.dateOfBirth)
                  : '-'}
              </span>
            ) : (
              <InputField
                data-qa="date-of-birth-input"
                onChange={setDateOfBirth}
                value={dateOfBirth}
              />
            )}
          </LabeledInput>
          <LabeledInput $cols={2}>
            <Label>Oletettu sukupuoli</Label>
            {props.mode === 'VIEW' ? (
              <span>
                {props.student.gender ? genderNames[props.student.gender] : '-'}
              </span>
            ) : (
              <Select<Gender>
                items={genders}
                selectedItem={gender}
                placeholder=" "
                getItemLabel={(item) => genderNames[item]}
                onChange={(item) => setGender(item)}
              />
            )}
          </LabeledInput>
          <LabeledInput $cols={3}>
            <Label>Sukunimi {props.mode !== 'VIEW' && '*'}</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.lastName}</span>
            ) : (
              <InputField
                data-qa="last-name-input"
                onChange={setLastName}
                value={lastName}
              />
            )}
          </LabeledInput>
          <LabeledInput $cols={3}>
            <Label>Etunimi {props.mode !== 'VIEW' && '*'}</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.firstName}</span>
            ) : (
              <InputField
                data-qa="first-name-input"
                onChange={setFirstName}
                value={firstName}
              />
            )}
          </LabeledInput>
        </RowOfInputs>
        <RowOfInputs>
          <LabeledInput $cols={2}>
            <Label>Puhelinnumero</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.phone || '-'}</span>
            ) : (
              <InputField onChange={setPhone} value={phone} />
            )}
          </LabeledInput>
          <LabeledInput $cols={4}>
            <Label>Sähköposti</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.email || '-'}</span>
            ) : (
              <InputField onChange={setEmail} value={email} />
            )}
          </LabeledInput>
          <LabeledInput $cols={6}>
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
          </LabeledInput>
        </RowOfInputs>
        {duplicateStudents.length > 0 && (
          <div>
            {duplicateSsnStudent ? (
              <AlertBox
                title="Samalla hetulla löytyy jo oppivelvollinen:"
                message={
                  <Link to={`/oppivelvolliset/${duplicateSsnStudent.id}`}>
                    {duplicateSsnStudent.name}
                  </Link>
                }
              />
            ) : duplicateValpasStudent ? (
              <AlertBox
                title="Samalla Valpas linkillä löytyy jo oppivelvollinen:"
                message={
                  <Link to={`/oppivelvolliset/${duplicateValpasStudent.id}`}>
                    {duplicateValpasStudent.name}
                  </Link>
                }
              />
            ) : (
              <InfoBox
                title={`Samalla nimellä löytyy jo ${
                  duplicateNameStudents.length > 1
                    ? 'oppivelvollisia'
                    : 'oppivelvollinen'
                }. Tarkista onko kyseessä sama henkilö:`}
                message={
                  <FlexColWithGaps>
                    {duplicateNameStudents.map((s) => (
                      <Link key={s.id} to={`/oppivelvolliset/${s.id}`}>
                        {s.name}
                        {s.dateOfBirth
                          ? ` (s. ${formatDate(s.dateOfBirth)})`
                          : ''}
                      </Link>
                    ))}
                  </FlexColWithGaps>
                }
              />
            )}
          </div>
        )}
        <RowOfInputs>
          <LabeledInput $cols={6}>
            <Label>Lähiosoite</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.address || '-'}</span>
            ) : (
              <InputField onChange={setAddress} value={address} />
            )}
          </LabeledInput>
          <LabeledInput $cols={3}>
            <Label>Kotikunta Suomessa</Label>
            {props.mode === 'VIEW' ? (
              <span>
                {props.student.municipalityInFinland ? 'Kyllä' : 'Ei'}
              </span>
            ) : (
              <Checkbox
                label="Kyllä"
                checked={municipalityInFinland}
                onChange={setMunicipalityInFinland}
              />
            )}
          </LabeledInput>
          <LabeledInput $cols={3}>
            <Label>Äidinkieli</Label>
            {props.mode === 'VIEW' ? (
              <span>{props.student.language}</span>
            ) : (
              <FlexRowWithGaps>
                <InputField onChange={setLanguage} value={language} />
                <InputQuickFill>
                  <Select<string>
                    items={commonLanguages}
                    selectedItem={null}
                    placeholder=" "
                    onChange={(item) => {
                      if (item) setLanguage(item)
                    }}
                  />
                </InputQuickFill>
              </FlexRowWithGaps>
            )}
          </LabeledInput>
        </RowOfInputs>
      </GroupOfInputRows>

      <VerticalGap $size="XL" />

      <H3>Huoltajat ja tukiverkosto</H3>
      <VerticalGap $size="m" />
      <GroupOfInputRows>
        <RowOfInputs>
          <LabeledInput $cols={6}>
            <Label>Huoltajat ja yhteystiedot</Label>
            {props.mode === 'VIEW' ? (
              <ReadOnlyTextArea text={props.student.guardianInfo || '-'} />
            ) : (
              <TextArea onChange={setGuardianInfo} value={guardianInfo} />
            )}
          </LabeledInput>
          <LabeledInput $cols={6}>
            <Label>Muut yhteyshenkilöt ja yhteystiedot</Label>
            {props.mode === 'VIEW' ? (
              <ReadOnlyTextArea
                text={props.student.supportContactsInfo || '-'}
              />
            ) : (
              <TextArea
                onChange={setSupportContactsInfo}
                value={supportContactsInfo}
              />
            )}
          </LabeledInput>
        </RowOfInputs>
      </GroupOfInputRows>
    </FlexCol>
  )
})

const InputQuickFill = styled.div`
  select {
    width: 12px;
  }
`
