import { Fragment, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import type { components } from '../api/types.gen'
import { describeError, isWriteUnconfirmed } from '../api/errors'
import { browserSession } from '../auth/browser'
import { useEscapeDismiss } from '../property/Editors'

type Schema = components['schemas']
type User = Schema['UserResponse']
type Role = Schema['RoleResponse']
type Editor = { mode: 'create' } | { mode: 'role' | 'deactivate'; user: User }
const api = browserSession.api
const accountSchema = z.object({
  name: z.string().refine((value) => value.trim().length > 0, 'errors.required'),
  email: z.string().email('errors.email').max(320, 'errors.email'),
  roleId: z.string().min(1, 'errors.required'),
})

async function roleOptions(signal: AbortSignal) {
  const roles: Role[] = []
  let page = 0
  let total = 1
  while (page < total) {
    const { data } = await api.get<Schema['PeoplePageRoleResponse']>('/api/roles', { params: { page, size: 100 }, signal })
    roles.push(...(data.content ?? []))
    total = data.totalPages ?? 0
    page++
  }
  return roles
}

function AccountForm({ pending, blocked, roles, close, submit }: {
  pending: boolean; blocked: boolean; roles: Role[]; close: () => void
  submit: (request: Schema['CreateUserRequest']) => void
}) {
  const { t } = useTranslation()
  useEscapeDismiss(pending, close)
  const form = useForm({ resolver: zodResolver(accountSchema), defaultValues: { name: '', email: '', roleId: '' } })
  return <form className="editor" noValidate onSubmit={form.handleSubmit((values) => { if (!pending && !blocked) submit(values) })}>
    <h2>{t('people.newUser')}</h2>
    {(['name', 'email'] as const).map((field) => {
      const error = form.formState.errors[field]?.message
      return <Fragment key={field}><label>{t(field)}<input type={field === 'email' ? 'email' : 'text'} autoComplete={field === 'email' ? 'email' : 'name'}
        aria-invalid={!!error} aria-describedby={error ? 'user-' + field + '-error' : undefined} {...form.register(field)} />
      </label>{error && <span className="error" id={'user-' + field + '-error'}>{t(error)}</span>}</Fragment>
    })}
    <label>{t('role')}<select aria-invalid={!!form.formState.errors.roleId} aria-describedby={form.formState.errors.roleId ? 'user-role-error' : undefined} {...form.register('roleId')}>
      <option value="">{t('people.selectRole')}</option>
      {roles.map((role) => <option key={role.id} value={role.id}>{role.name}</option>)}
    </select></label>{form.formState.errors.roleId && <span className="error" id="user-role-error">{t('errors.required')}</span>}
    <p>{t('people.firstUseHelp')}</p>
    <div className="row"><button type="button" className="secondary" disabled={pending} onClick={close}>{t('cancel')}</button>
      <button disabled={pending || blocked}>{t(pending ? 'saving' : 'people.create')}</button></div>
  </form>
}

function ChangeAccount({ editor, roles, pending, blocked, close, submit }: {
  editor: Exclude<Editor, { mode: 'create' }>; roles: Role[]; pending: boolean; blocked: boolean
  close: () => void; submit: (roleId?: string) => void
}) {
  const { t } = useTranslation()
  const [roleId, setRoleId] = useState(editor.user.roleId ?? '')
  useEscapeDismiss(pending, close)
  return <form className="editor" onSubmit={(event) => { event.preventDefault(); if (!pending && !blocked) submit(roleId) }}>
    <h2>{t(editor.mode === 'role' ? 'people.changeRoleFor' : 'people.deactivateFor', { name: editor.user.name })}</h2>
    {editor.mode === 'role' ? <label>{t('role')}<select value={roleId} required onChange={(event) => setRoleId(event.target.value)}>
      {roles.map((role) => <option key={role.id} value={role.id}>{role.name}</option>)}
    </select></label> : <p>{t('people.deactivateHelp')}</p>}
    <div className="row"><button className="secondary" type="button" disabled={pending} onClick={close}>{t('cancel')}</button>
      <button disabled={pending || blocked || (editor.mode === 'role' && !roleId)}>{t(pending ? 'saving' : editor.mode === 'role' ? 'save' : 'people.deactivate')}</button></div>
  </form>
}

export function Users() {
  const { t } = useTranslation()
  const queries = useQueryClient()
  const trigger = useRef<HTMLButtonElement | null>(null)
  const [page, setPage] = useState(0)
  const [editor, setEditor] = useState<Editor>()
  const [issued, setIssued] = useState<{ email: string; password: string }>()
  const [notice, setNotice] = useState('')
  const users = useQuery({ queryKey: ['people', 'users', page], queryFn: async ({ signal }) =>
    (await api.get<Schema['PeoplePageUserResponse']>('/api/users', { params: { page, size: 20 }, signal })).data })
  const roles = useQuery({ queryKey: ['people', 'role-options'], queryFn: ({ signal }) => roleOptions(signal) })
  function edit(next?: Editor, button?: HTMLButtonElement) {
    if (button) trigger.current = button
    action.reset()
    setEditor(next)
    setNotice('')
    if (!next && trigger.current?.isConnected) trigger.current.focus()
  }
  const action = useMutation({
    gcTime: 0,
    mutationFn: async (request: Schema['CreateUserRequest'] | string | undefined) => {
      if (editor?.mode === 'create') {
        const { data } = await api.post<Schema['CreatedUserResponse']>('/api/users', request)
        if (!data.temporaryPassword || !data.user?.email) throw new Error('Invalid account response')
        setIssued({ email: data.user.email, password: data.temporaryPassword })
      } else if (editor?.mode === 'role') await api.put('/api/users/' + editor.user.id + '/role', { roleId: request })
      else if (editor?.mode === 'deactivate') await api.post('/api/users/' + editor.user.id + '/deactivate')
    },
    onSuccess: () => {
      edit()
      setNotice('people.saved')
      void queries.invalidateQueries({ queryKey: ['people', 'users'] })
    },
  })
  const blocked = isWriteUnconfirmed(action.error)
  const error = describeError(action.error, 'people')
  return <section className="card">
    <div className="row between"><h1>{t('people.users')}</h1><button disabled={action.isPending || !!issued || !roles.data?.length}
      onClick={(event) => edit({ mode: 'create' }, event.currentTarget)}>{t('people.newUser')}</button></div>
    {issued && <section className="editor" aria-label={t('people.credentials')}>
      <h2>{t('people.createdFor', { email: issued.email })}</h2><p>{t('people.passwordHelp')}</p>
      <label>{t('people.temporaryPassword')}<input readOnly autoComplete="off" spellCheck={false} value={issued.password} /></label>
      <button className="secondary" onClick={() => { setIssued(undefined); trigger.current?.focus() }}>{t('people.dismissPassword')}</button>
    </section>}
    {notice && <p role="status">{t(notice)}</p>}
    {action.isError && <div className="callout" data-tone={blocked ? 'warning' : 'danger'}>
      <p role="alert">{t(blocked ? editor?.mode === 'create' ? 'people.createUnconfirmed' : 'errors.writeUnconfirmed' : error.key, error)}</p>
      {blocked && <button className="secondary" onClick={() => void users.refetch()}>{t('refreshView')}</button>}
    </div>}
    {editor?.mode === 'create' && <AccountForm pending={action.isPending} blocked={blocked} roles={roles.data ?? []} close={() => edit()} submit={(body) => action.mutate(body)} />}
    {editor && editor.mode !== 'create' && <ChangeAccount key={editor.mode + editor.user.id} editor={editor} roles={roles.data ?? []}
      pending={action.isPending} blocked={blocked} close={() => edit()} submit={(roleId) => action.mutate(roleId)} />}
    {users.isPending && <p role="status">{t('people.loading')}</p>}
    {users.isError && <div className="callout" data-tone="danger"><p role="alert">{t('people.loadFailed')}</p><button className="secondary" onClick={() => void users.refetch()}>{t('retry')}</button></div>}
    {roles.isError && <div className="callout" data-tone="danger"><p role="alert">{t('people.rolesFailed')}</p><button className="secondary" onClick={() => void roles.refetch()}>{t('retry')}</button></div>}
    {users.data && (users.data.content?.length ? <div className="table-wrap"><table aria-label={t('people.users')}>
      <thead><tr>{['name', 'email', 'role', 'people.status', 'actions'].map((key) => <th scope="col" key={key}>{t(key)}</th>)}</tr></thead>
      <tbody>{users.data.content.map((user) => <tr key={user.id}><td>{user.name}</td><td>{user.email}</td><td>{user.roleName}</td>
        <td>{t(user.status === 'ACTIVE' ? 'people.active' : 'people.inactive')}</td><td><div className="row">
          <button className="secondary" disabled={action.isPending || !!issued || !roles.data?.length} aria-label={t('people.changeRoleFor', { name: user.name })}
            onClick={(event) => edit({ mode: 'role', user }, event.currentTarget)}>{t('people.changeRole')}</button>
          {user.status === 'ACTIVE' && <button className="secondary" disabled={action.isPending || !!issued} aria-label={t('people.deactivateFor', { name: user.name })}
            onClick={(event) => edit({ mode: 'deactivate', user }, event.currentTarget)}>{t('people.deactivate')}</button>}
        </div></td></tr>)}</tbody>
    </table></div> : <p>{t('people.empty')}</p>)}
    <div className="row"><button className="secondary" disabled={page === 0 || users.isFetching || action.isPending} onClick={() => setPage(page - 1)}>{t('previous')}</button>
      <span>{t('page', { current: page + 1, total: Math.max(1, users.data?.totalPages ?? 0) })}</span>
      <button className="secondary" disabled={users.isFetching || action.isPending || page + 1 >= (users.data?.totalPages ?? 0)} onClick={() => setPage(page + 1)}>{t('next')}</button></div>
  </section>
}
