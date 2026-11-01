import { zodResolver } from '@hookform/resolvers/zod'
import { Boxes } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCurrentUser, useLogin } from '@/hooks/use-auth'
import { ApiRequestError } from '@/lib/api-client'

const schema = z.object({
  username: z.string().trim().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})
type FormValues = z.infer<typeof schema>

/** Sign-in form. There is deliberately no self-registration (US-04). */
export default function LoginPage() {
  const { data: user } = useCurrentUser()
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  if (user) return <Navigate to="/" replace />

  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/'
  const onSubmit = (values: FormValues) =>
    login.mutate(values, { onSuccess: () => navigate(from, { replace: true }) })
  const failed401 = login.error instanceof ApiRequestError && login.error.status === 401

  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <div className="mb-2 flex size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Boxes className="size-5" />
          </div>
          <CardTitle>Sign in to WasteMS</CardTitle>
          <CardDescription>Accounts are created by an administrator.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
            {login.isError && (
              <Alert variant="destructive">
                <AlertDescription>
                  {failed401 ? 'Invalid username or password.' : 'Sign-in failed. Please try again.'}
                </AlertDescription>
              </Alert>
            )}
            <div className="flex flex-col gap-2">
              <Label htmlFor="username">Username</Label>
              <Input id="username" autoComplete="username" {...register('username')} />
              {errors.username && (
                <p className="text-sm text-destructive">{errors.username.message}</p>
              )}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                {...register('password')}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
            </div>
            <Button type="submit" disabled={login.isPending}>
              {login.isPending ? 'Signing in...' : 'Sign in'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  )
}
